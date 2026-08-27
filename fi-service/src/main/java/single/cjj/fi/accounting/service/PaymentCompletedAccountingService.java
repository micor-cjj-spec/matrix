package single.cjj.fi.accounting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.accounting.integration.BusinessEventEnvelope;
import single.cjj.fi.accounting.model.AccountingModels.AccountingLine;
import single.cjj.fi.accounting.model.AccountingModels.EventContext;
import single.cjj.fi.accounting.model.AccountingModels.ProcessingResult;
import single.cjj.fi.accounting.model.AccountingModels.RuleEvaluation;
import single.cjj.fi.accounting.persistence.InboundAccountingRepository;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.SettlementRow;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.service.BizfiFiVoucherService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentCompletedAccountingService {

    public static final String BUSINESS_EVENT_TYPE = "PAYMENT_COMPLETED";
    public static final String ACCOUNTING_EVENT_TYPE = "PURCHASE_PAYMENT_RECOGNITION";

    private final ObjectMapper objectMapper;
    private final InboundAccountingRepository commonRepository;
    private final PaymentSettlementRepository settlementRepository;
    private final AccountingRuleEngine ruleEngine;
    private final BizfiFiVoucherService voucherService;
    private final BizfiFiVoucherMapper voucherMapper;
    private final String consumerCode;
    private final String defaultBookId;

    public PaymentCompletedAccountingService(
            ObjectMapper objectMapper,
            InboundAccountingRepository commonRepository,
            PaymentSettlementRepository settlementRepository,
            AccountingRuleEngine ruleEngine,
            BizfiFiVoucherService voucherService,
            BizfiFiVoucherMapper voucherMapper,
            @Value("${fi.accounting.payment-completed-consumer-code:FI_PAYMENT_COMPLETED_ACCOUNTING_V1}")
            String consumerCode,
            @Value("${fi.accounting.default-book-id:DEFAULT}")
            String defaultBookId
    ) {
        this.objectMapper = objectMapper;
        this.commonRepository = commonRepository;
        this.settlementRepository = settlementRepository;
        this.ruleEngine = ruleEngine;
        this.voucherService = voucherService;
        this.voucherMapper = voucherMapper;
        this.consumerCode = consumerCode;
        this.defaultBookId = defaultBookId;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessingResult process(String rawJson) {
        BusinessEventEnvelope event = BusinessEventEnvelope.parse(objectMapper, rawJson);
        validateEnvelope(event);

        String inboxStatus = commonRepository.findInboxStatus(
                consumerCode, event.eventId());
        if ("PROCESSED".equals(inboxStatus)) {
            return commonRepository.findInboxResult(
                    consumerCode, event.eventId());
        }
        if ("FAILED".equals(inboxStatus)) {
            commonRepository.resetInboxProcessing(
                    consumerCode, event.eventId());
        } else if (inboxStatus == null) {
            int inserted = commonRepository.insertInbox(
                    IdWorker.getId(), consumerCode, event);
            if (inserted == 0) {
                return commonRepository.findInboxResult(
                        consumerCode, event.eventId());
            }
        } else if ("PROCESSING".equals(inboxStatus)) {
            return commonRepository.findInboxResult(
                    consumerCode, event.eventId());
        }

        PaymentSnapshot snapshot = validatePayload(event);
        SettlementRow settlement = settlementRepository.findSettlement(
                snapshot.settlementId(), event.tenantId());
        validateSettlement(event, snapshot, settlement);

        Long accountingEventPk = IdWorker.getId();
        String accountingEventId = newAccountingEventId();
        String factsJson = toJson(event.payload());

        commonRepository.insertAccountingEvent(
                accountingEventPk,
                accountingEventId,
                ACCOUNTING_EVENT_TYPE,
                event,
                defaultBookId,
                factsJson
        );

        EventContext context = new EventContext(
                event.tenantId(),
                event.orgId(),
                defaultBookId,
                event.businessDate() == null
                        ? LocalDate.now()
                        : event.businessDate(),
                event.sourceDocumentNo(),
                event.payload()
        );
        RuleEvaluation evaluation = ruleEngine.evaluate(
                ACCOUNTING_EVENT_TYPE, context);

        persistAccountingResult(accountingEventPk, evaluation);
        commonRepository.markAccountingReady(
                accountingEventPk,
                evaluation.rule(),
                sha256(factsJson)
        );

        BizfiFiVoucher voucher = createOrReuseVoucherDraft(
                event, accountingEventId, evaluation);
        List<BizfiFiVoucherLine> voucherLines =
                voucherService.listLines(voucher.getFid());
        persistVoucherDimensions(
                voucher.getFid(), voucherLines, evaluation.lines());

        commonRepository.markAccountingVoucherGenerated(
                accountingEventPk,
                voucher.getFid(),
                voucher.getFnumber()
        );
        settlementRepository.updateSettlementAccounting(
                settlement.id(),
                event.tenantId(),
                accountingEventId,
                voucher.getFid(),
                voucher.getFnumber()
        );
        settlementRepository.insertPaymentAccountingTrace(
                IdWorker.getId(),
                event.tenantId(),
                event.orgId(),
                event.eventId(),
                event.eventType(),
                event.sourceDocumentId(),
                event.sourceDocumentNo(),
                settlement.id(),
                snapshot.paymentOrderId(),
                snapshot.bankTransactionId(),
                accountingEventId,
                evaluation.rule().ruleCode(),
                evaluation.rule().versionNo(),
                voucher.getFid(),
                voucher.getFnumber()
        );

        commonRepository.markInboxProcessed(
                consumerCode,
                event.eventId(),
                null,
                accountingEventId,
                voucher.getFid(),
                voucher.getFnumber()
        );

        return new ProcessingResult(
                false,
                null,
                accountingEventId,
                voucher.getFid(),
                voucher.getFnumber()
        );
    }

    private void validateEnvelope(BusinessEventEnvelope event) {
        if (!BUSINESS_EVENT_TYPE.equals(event.eventType())) {
            throw new BizException(
                    "BUSINESS_EVENT_TYPE_UNSUPPORTED: " + event.eventType());
        }
        if (event.eventVersion() != 1) {
            throw new BizException(
                    "BUSINESS_EVENT_VERSION_UNSUPPORTED: " + event.eventVersion());
        }
        if (!"fi-service".equals(event.producerService())) {
            throw new BizException(
                    "PAYMENT_COMPLETED producer must be fi-service");
        }
        if (!"FI_PAYMENT_ORDER".equals(event.sourceDocumentType())) {
            throw new BizException(
                    "PAYMENT_COMPLETED sourceDocumentType must be FI_PAYMENT_ORDER");
        }
        if (event.orgId() == null) {
            throw new BizException("PAYMENT_COMPLETED 缺少 orgId");
        }
    }

    private PaymentSnapshot validatePayload(BusinessEventEnvelope event) {
        JsonNode payload = event.payload();
        if (payload == null || !payload.isObject()) {
            throw new BizException("PAYMENT_COMPLETED payload 必须是对象");
        }
        Long paymentOrderId = requiredLong(payload, "paymentOrderId");
        Long settlementId = requiredLong(payload, "settlementId");
        Long bankTransactionId = requiredLong(payload, "bankTransactionId");
        String paymentOrderNo = requiredText(payload, "paymentOrderNo");
        String settlementNo = requiredText(payload, "settlementNo");
        String businessPartnerId = requiredText(payload, "businessPartnerId");
        String currencyCode = requiredText(payload, "currencyCode");
        String payerBankAccountId = requiredText(payload, "payerBankAccountId");
        BigDecimal amount = requiredMoney(payload, "amount");

        if (!String.valueOf(paymentOrderId).equals(event.sourceDocumentId())) {
            throw new BizException(
                    "PAYMENT_COMPLETED sourceDocumentId 与 paymentOrderId 不一致");
        }
        if (event.sourceDocumentNo() != null
                && !event.sourceDocumentNo().equals(paymentOrderNo)) {
            throw new BizException(
                    "PAYMENT_COMPLETED sourceDocumentNo 与 paymentOrderNo 不一致");
        }

        JsonNode entries = payload.path("entries");
        if (!entries.isArray() || entries.isEmpty()) {
            throw new BizException("PAYMENT_COMPLETED 缺少 settlement entries");
        }
        BigDecimal entryTotal = BigDecimal.ZERO.setScale(2);
        for (JsonNode entry : entries) {
            requiredLong(entry, "settlementEntryId");
            requiredLong(entry, "payableId");
            requiredLong(entry, "paymentApplicationId");
            requiredText(entry, "payableNumber");
            entryTotal = entryTotal.add(
                    requiredMoney(entry, "settledAmount"));
        }
        entryTotal = entryTotal.setScale(2, RoundingMode.HALF_UP);
        if (entryTotal.compareTo(amount) != 0) {
            throw new BizException(
                    "PAYMENT_COMPLETED entries 合计与 amount 不一致");
        }

        return new PaymentSnapshot(
                paymentOrderId,
                paymentOrderNo,
                settlementId,
                settlementNo,
                bankTransactionId,
                businessPartnerId,
                currencyCode,
                payerBankAccountId,
                amount
        );
    }

    private void validateSettlement(
            BusinessEventEnvelope event,
            PaymentSnapshot snapshot,
            SettlementRow settlement
    ) {
        if (settlement == null) {
            throw new BizException(
                    "PAYMENT_COMPLETED settlement 不存在: "
                            + snapshot.settlementId());
        }
        if (!"COMPLETED".equals(settlement.status())) {
            throw new BizException("PAYMENT_COMPLETED settlement 未完成");
        }
        if (!snapshot.paymentOrderId().equals(settlement.paymentOrderId())
                || !snapshot.bankTransactionId().equals(settlement.bankTransactionId())) {
            throw new BizException(
                    "PAYMENT_COMPLETED settlement 付款/银行引用不一致");
        }
        if (!snapshot.businessPartnerId().equals(settlement.businessPartnerId())
                || !snapshot.currencyCode().equals(settlement.currencyCode())) {
            throw new BizException(
                    "PAYMENT_COMPLETED settlement 客商/币种不一致");
        }
        if (snapshot.amount().compareTo(
                settlement.amount().setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new BizException(
                    "PAYMENT_COMPLETED settlement 金额不一致");
        }
        if (settlement.businessEventId() != null
                && !settlement.businessEventId().equals(event.eventId())) {
            throw new BizException(
                    "PAYMENT_COMPLETED eventId 与 settlement 不一致");
        }
    }

    private void persistAccountingResult(
            Long accountingEventPk,
            RuleEvaluation evaluation
    ) {
        Map<Integer, Long> ids = evaluation.lines()
                .stream()
                .collect(Collectors.toMap(
                        AccountingLine::lineNo,
                        ignored -> IdWorker.getId()));
        for (AccountingLine line : evaluation.lines()) {
            Long entryId = ids.get(line.lineNo());
            commonRepository.insertAccountingEntry(
                    entryId, accountingEventPk, line);
            for (var dimension : line.dimensions()) {
                commonRepository.insertAccountingDimension(
                        IdWorker.getId(),
                        accountingEventPk,
                        entryId,
                        dimension.code(),
                        dimension.valueId(),
                        dimension.valueCode(),
                        dimension.valueName()
                );
            }
        }
    }

    private BizfiFiVoucher createOrReuseVoucherDraft(
            BusinessEventEnvelope event,
            String accountingEventId,
            RuleEvaluation evaluation
    ) {
        String sourceRequestId =
                "ACCOUNTING:" + accountingEventId + ":VOUCHER:0";
        BizfiFiVoucher existing = voucherMapper.selectOne(
                new LambdaQueryWrapper<BizfiFiVoucher>()
                        .eq(BizfiFiVoucher::getTenantId, event.tenantId())
                        .eq(BizfiFiVoucher::getSourceRequestId, sourceRequestId)
                        .last("limit 1")
        );
        if (existing != null) {
            return existing;
        }

        BizfiFiVoucher voucher = new BizfiFiVoucher();
        voucher.setTenantId(event.tenantId());
        voucher.setOrganizationId(
                event.orgId() == null ? null : String.valueOf(event.orgId()));
        voucher.setBookId(defaultBookId);
        voucher.setSourceRequestId(sourceRequestId);
        voucher.setFdate(
                event.businessDate() == null
                        ? LocalDate.now()
                        : event.businessDate());
        voucher.setFsummary(
                "采购付款-" + safe(event.sourceDocumentNo()));
        voucher.setFamount(evaluation.debitTotal());
        voucher.setFcreatedBy("accounting-engine");
        voucher.setFremark(
                "AccountingEvent="
                        + accountingEventId
                        + "; BusinessEvent="
                        + event.eventId());
        voucherService.saveDraft(voucher);

        List<BizfiFiVoucherLine> lines = new ArrayList<>();
        for (AccountingLine resultLine : evaluation.lines()) {
            BizfiFiVoucherLine line = new BizfiFiVoucherLine();
            line.setFlineNo(resultLine.lineNo());
            line.setFaccountCode(resultLine.accountCode());
            line.setFsummary(resultLine.summary());
            line.setFdebitAmount(resultLine.debitAmount());
            line.setFcreditAmount(resultLine.creditAmount());
            line.setFcurrency(resultLine.currencyCode());
            line.setFrate(BigDecimal.ONE);
            line.setForiginalAmount(resultLine.originalAmount());
            lines.add(line);
        }
        voucherService.saveLines(voucher.getFid(), lines);
        return voucherService.get(voucher.getFid());
    }

    private void persistVoucherDimensions(
            Long voucherId,
            List<BizfiFiVoucherLine> voucherLines,
            List<AccountingLine> accountingLines
    ) {
        Map<Integer, BizfiFiVoucherLine> byLineNo =
                voucherLines.stream().collect(Collectors.toMap(
                        BizfiFiVoucherLine::getFlineNo,
                        Function.identity(),
                        (first, ignored) -> first
                ));
        for (AccountingLine accountingLine : accountingLines) {
            BizfiFiVoucherLine voucherLine =
                    byLineNo.get(accountingLine.lineNo());
            if (voucherLine == null) {
                throw new BizException(
                        "VOUCHER_LINE_NOT_FOUND: "
                                + accountingLine.lineNo());
            }
            for (var dimension : accountingLine.dimensions()) {
                commonRepository.insertVoucherLineDimension(
                        IdWorker.getId(),
                        voucherId,
                        voucherLine.getFid(),
                        dimension.code(),
                        dimension.valueId(),
                        dimension.valueCode(),
                        dimension.valueName()
                );
            }
        }
    }

    private String newAccountingEventId() {
        return "AE-PAY-"
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .toUpperCase();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BizException(
                    "PAYMENT_COMPLETED accounting payload 序列化失败");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "SHA-256 unavailable", exception);
        }
    }

    private Long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new BizException(
                    "PAYMENT_COMPLETED 缺少字段: " + field);
        }
        if (!value.canConvertToLong()) {
            throw new BizException(
                    "PAYMENT_COMPLETED 字段不是Long: " + field);
        }
        return value.asLong();
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        String text = value == null || value.isNull()
                ? null
                : value.asText();
        if (text == null || text.isBlank()) {
            throw new BizException(
                    "PAYMENT_COMPLETED 缺少字段: " + field);
        }
        return text;
    }

    private BigDecimal requiredMoney(JsonNode node, String field) {
        String value = requiredText(node, field);
        try {
            BigDecimal amount =
                    new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(
                        "PAYMENT_COMPLETED 金额必须大于0: " + field);
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw new BizException(
                    "PAYMENT_COMPLETED 金额格式错误: " + field);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record PaymentSnapshot(
            Long paymentOrderId,
            String paymentOrderNo,
            Long settlementId,
            String settlementNo,
            Long bankTransactionId,
            String businessPartnerId,
            String currencyCode,
            String payerBankAccountId,
            BigDecimal amount
    ) {
    }
}
