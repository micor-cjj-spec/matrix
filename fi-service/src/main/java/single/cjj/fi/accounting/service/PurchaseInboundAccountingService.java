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
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PurchaseInboundAccountingService {

    public static final String BUSINESS_EVENT_TYPE = "PURCHASE_INBOUND_CONFIRMED";
    public static final String ACCOUNTING_EVENT_TYPE = "PURCHASE_INBOUND_ESTIMATE_RECOGNITION";

    private final ObjectMapper objectMapper;
    private final InboundAccountingRepository repository;
    private final AccountingRuleEngine ruleEngine;
    private final BizfiFiVoucherService voucherService;
    private final BizfiFiVoucherMapper voucherMapper;
    private final String consumerCode;
    private final String defaultBookId;

    public PurchaseInboundAccountingService(
            ObjectMapper objectMapper,
            InboundAccountingRepository repository,
            AccountingRuleEngine ruleEngine,
            BizfiFiVoucherService voucherService,
            BizfiFiVoucherMapper voucherMapper,
            @Value("${fi.accounting.purchase-inbound-consumer-code:FI_PURCHASE_INBOUND_ACCOUNTING_V1}") String consumerCode,
            @Value("${fi.accounting.default-book-id:DEFAULT}") String defaultBookId
    ) {
        this.objectMapper = objectMapper;
        this.repository = repository;
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

        String inboxStatus = repository.findInboxStatus(consumerCode, event.eventId());
        if ("PROCESSED".equals(inboxStatus)) {
            return repository.findInboxResult(consumerCode, event.eventId());
        }
        if ("FAILED".equals(inboxStatus)) {
            repository.resetInboxProcessing(consumerCode, event.eventId());
        } else if (inboxStatus == null) {
            int inserted = repository.insertInbox(IdWorker.getId(), consumerCode, event);
            if (inserted == 0) {
                return repository.findInboxResult(consumerCode, event.eventId());
            }
        } else if ("PROCESSING".equals(inboxStatus)) {
            return repository.findInboxResult(consumerCode, event.eventId());
        }

        PayloadSnapshot snapshot = validatePayload(event.payload());
        Long payableId = repository.findPayableIdByEvent(event.tenantId(), event.eventId());
        if (payableId == null) {
            payableId = createEstimatePayable(event, snapshot);
        }

        Long accountingEventPk = IdWorker.getId();
        String accountingEventId = newAccountingEventId();
        String factsJson = toJson(event.payload());
        repository.insertAccountingEvent(
                accountingEventPk,
                accountingEventId,
                ACCOUNTING_EVENT_TYPE,
                event,
                defaultBookId,
                factsJson
        );

        EventContext context = new EventContext(
                event.tenantId(), event.orgId(), defaultBookId,
                event.businessDate() == null ? LocalDate.now() : event.businessDate(),
                event.sourceDocumentNo(), event.payload()
        );
        RuleEvaluation evaluation = ruleEngine.evaluate(ACCOUNTING_EVENT_TYPE, context);

        Map<Integer, Long> accountingEntryIds = persistAccountingResult(accountingEventPk, evaluation);
        repository.markAccountingReady(accountingEventPk, evaluation.rule(), sha256(factsJson));

        BizfiFiVoucher voucher = createOrReuseVoucherDraft(event, accountingEventId, evaluation);
        List<BizfiFiVoucherLine> voucherLines = voucherService.listLines(voucher.getFid());
        persistVoucherDimensions(voucher.getFid(), voucherLines, evaluation.lines());

        repository.markAccountingVoucherGenerated(accountingEventPk, voucher.getFid(), voucher.getFnumber());
        repository.updatePayableAccounting(payableId, accountingEventId, voucher.getFid(), voucher.getFnumber());
        repository.insertTrace(
                IdWorker.getId(), event, payableId, accountingEventId,
                evaluation.rule().ruleCode(), evaluation.rule().versionNo(), voucher.getFid(), voucher.getFnumber());
        repository.markInboxProcessed(
                consumerCode, event.eventId(), payableId, accountingEventId, voucher.getFid(), voucher.getFnumber());

        return new ProcessingResult(false, payableId, accountingEventId, voucher.getFid(), voucher.getFnumber());
    }

    private Long createEstimatePayable(BusinessEventEnvelope event, PayloadSnapshot snapshot) {
        Long payableId = IdWorker.getId();
        repository.insertPayable(
                payableId,
                event,
                buildPayableNumber(event),
                requiredText(event.payload(), "businessPartnerId"),
                nullableText(event.payload().get("businessPartnerCode")),
                nullableText(event.payload().get("businessPartnerName")),
                requiredText(event.payload(), "currencyCode"),
                snapshot.totalAmount()
        );
        int lineNo = 1;
        for (JsonNode entry : event.payload().path("entries")) {
            repository.insertPayableEntry(
                    IdWorker.getId(), payableId, event, lineNo++,
                    requiredText(entry, "inboundEntryId"),
                    nullableText(entry.get("purchaseOrderId")),
                    nullableText(entry.get("purchaseOrderEntryId")),
                    nullableText(entry.get("materialId")),
                    nullableText(entry.get("materialCode")),
                    nullableText(entry.get("materialName")),
                    decimal(entry.get("quantity"), "entry.quantity"),
                    decimal(entry.get("unitPrice"), "entry.unitPrice"),
                    decimal(entry.get("amount"), "entry.amount").setScale(2, RoundingMode.HALF_UP),
                    nullableText(entry.get("warehouseId")),
                    nullableText(entry.get("projectId")),
                    nullableText(entry.get("costCenterId"))
            );
        }
        return payableId;
    }

    private Map<Integer, Long> persistAccountingResult(Long accountingEventPk, RuleEvaluation evaluation) {
        Map<Integer, Long> ids = evaluation.lines().stream()
                .collect(Collectors.toMap(AccountingLine::lineNo, ignored -> IdWorker.getId()));
        for (AccountingLine line : evaluation.lines()) {
            Long entryId = ids.get(line.lineNo());
            repository.insertAccountingEntry(entryId, accountingEventPk, line);
            for (var dimension : line.dimensions()) {
                repository.insertAccountingDimension(
                        IdWorker.getId(), accountingEventPk, entryId,
                        dimension.code(), dimension.valueId(), dimension.valueCode(), dimension.valueName());
            }
        }
        return ids;
    }

    private BizfiFiVoucher createOrReuseVoucherDraft(
            BusinessEventEnvelope event,
            String accountingEventId,
            RuleEvaluation evaluation
    ) {
        String sourceRequestId = "ACCOUNTING:" + accountingEventId + ":VOUCHER:0";
        BizfiFiVoucher existing = voucherMapper.selectOne(new LambdaQueryWrapper<BizfiFiVoucher>()
                .eq(BizfiFiVoucher::getTenantId, event.tenantId())
                .eq(BizfiFiVoucher::getSourceRequestId, sourceRequestId)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }

        BizfiFiVoucher voucher = new BizfiFiVoucher();
        voucher.setTenantId(event.tenantId());
        voucher.setOrganizationId(event.orgId() == null ? null : String.valueOf(event.orgId()));
        voucher.setBookId(defaultBookId);
        voucher.setSourceRequestId(sourceRequestId);
        voucher.setFdate(event.businessDate() == null ? LocalDate.now() : event.businessDate());
        voucher.setFsummary("采购入库暂估-" + safe(event.sourceDocumentNo()));
        voucher.setFamount(evaluation.debitTotal());
        voucher.setFcreatedBy("accounting-engine");
        voucher.setFremark("AccountingEvent=" + accountingEventId + "; BusinessEvent=" + event.eventId());
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
        Map<Integer, BizfiFiVoucherLine> byLineNo = voucherLines.stream()
                .collect(Collectors.toMap(BizfiFiVoucherLine::getFlineNo, Function.identity(), (a, b) -> a));
        for (AccountingLine accountingLine : accountingLines) {
            BizfiFiVoucherLine voucherLine = byLineNo.get(accountingLine.lineNo());
            if (voucherLine == null) {
                throw new BizException("VOUCHER_LINE_NOT_FOUND: " + accountingLine.lineNo());
            }
            for (var dimension : accountingLine.dimensions()) {
                repository.insertVoucherLineDimension(
                        IdWorker.getId(), voucherId, voucherLine.getFid(),
                        dimension.code(), dimension.valueId(), dimension.valueCode(), dimension.valueName());
            }
        }
    }

    private PayloadSnapshot validatePayload(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new BizException("BUSINESS_EVENT_PAYLOAD_INVALID");
        }
        requiredText(payload, "businessPartnerId");
        requiredText(payload, "currencyCode");
        BigDecimal total = decimal(payload.get("totalAmount"), "payload.totalAmount").setScale(2, RoundingMode.HALF_UP);
        JsonNode entries = payload.path("entries");
        if (!entries.isArray() || entries.isEmpty()) {
            throw new BizException("BUSINESS_EVENT_PAYLOAD_INVALID: entries empty");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode entry : entries) {
            requiredText(entry, "inboundEntryId");
            BigDecimal lineAmount = decimal(entry.get("amount"), "entry.amount").setScale(2, RoundingMode.HALF_UP);
            if (lineAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("BUSINESS_EVENT_PAYLOAD_INVALID: entry.amount must be > 0");
            }
            sum = sum.add(lineAmount);
        }
        sum = sum.setScale(2, RoundingMode.HALF_UP);
        if (sum.subtract(total).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BizException("BUSINESS_EVENT_AMOUNT_MISMATCH: total=" + total + ", entries=" + sum);
        }
        return new PayloadSnapshot(total, sum);
    }

    private void validateEnvelope(BusinessEventEnvelope event) {
        if (!BUSINESS_EVENT_TYPE.equals(event.eventType())) {
            throw new BizException("UNSUPPORTED_BUSINESS_EVENT: " + event.eventType());
        }
        if (event.eventVersion() != 1) {
            throw new BizException("UNSUPPORTED_BUSINESS_EVENT_VERSION: " + event.eventVersion());
        }
        if (!"ERP_PURCHASE_INBOUND".equals(event.sourceDocumentType())) {
            throw new BizException("UNSUPPORTED_SOURCE_DOCUMENT: " + event.sourceDocumentType());
        }
    }

    private String buildPayableNumber(BusinessEventEnvelope event) {
        String source = event.sourceDocumentNo();
        if (source == null || source.isBlank()) {
            source = event.sourceDocumentId();
        }
        String number = "APEST-" + source;
        return number.length() <= 100 ? number : number.substring(0, 100);
    }

    private String newAccountingEventId() {
        return "AE-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new BizException("SOURCE_PAYLOAD_HASH_FAILED");
        }
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            throw new BizException("ACCOUNTING_FACTS_SERIALIZE_FAILED");
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = nullableText(node.get(field));
        if (value == null || value.isBlank()) {
            throw new BizException("BUSINESS_EVENT_PAYLOAD_MISSING: " + field);
        }
        return value;
    }

    private String nullableText(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new BizException("BUSINESS_EVENT_PAYLOAD_MISSING: " + field);
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException exception) {
            throw new BizException("BUSINESS_EVENT_PAYLOAD_INVALID_NUMBER: " + field);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record PayloadSnapshot(BigDecimal totalAmount, BigDecimal entryAmount) {
    }
}
