package single.cjj.fi.ap.deduction;

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
import single.cjj.fi.accounting.service.AccountingRuleEngine;
import single.cjj.fi.ap.deduction.PurchaseDeductionAccountingRepository.PayableCandidate;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.service.BizfiFiVoucherService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PurchaseDeductionAccountingService {

    public static final String BUSINESS_EVENT_TYPE = "PURCHASE_DEDUCTION_CONFIRMED";
    public static final String ACCOUNTING_EVENT_TYPE = "PURCHASE_DEDUCTION_RECOGNITION";

    private final ObjectMapper objectMapper;
    private final InboundAccountingRepository commonRepository;
    private final PurchaseDeductionAccountingRepository repository;
    private final AccountingRuleEngine ruleEngine;
    private final BizfiFiVoucherService voucherService;
    private final BizfiFiVoucherMapper voucherMapper;
    private final String consumerCode;
    private final String defaultBookId;

    public PurchaseDeductionAccountingService(
            ObjectMapper objectMapper,
            InboundAccountingRepository commonRepository,
            PurchaseDeductionAccountingRepository repository,
            AccountingRuleEngine ruleEngine,
            BizfiFiVoucherService voucherService,
            BizfiFiVoucherMapper voucherMapper,
            @Value("${fi.accounting.purchase-deduction-consumer-code:FI_PURCHASE_DEDUCTION_ACCOUNTING_V1}")
            String consumerCode,
            @Value("${fi.accounting.default-book-id:DEFAULT}")
            String defaultBookId
    ) {
        this.objectMapper = objectMapper;
        this.commonRepository = commonRepository;
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

        String inboxStatus = commonRepository.findInboxStatus(consumerCode, event.eventId());
        if ("PROCESSED".equals(inboxStatus)) {
            return commonRepository.findInboxResult(consumerCode, event.eventId());
        }
        if ("FAILED".equals(inboxStatus)) {
            commonRepository.resetInboxProcessing(consumerCode, event.eventId());
        } else if (inboxStatus == null) {
            int inserted = commonRepository.insertInbox(IdWorker.getId(), consumerCode, event);
            if (inserted == 0) {
                return commonRepository.findInboxResult(consumerCode, event.eventId());
            }
        } else if ("PROCESSING".equals(inboxStatus)) {
            return commonRepository.findInboxResult(consumerCode, event.eventId());
        }

        DeductionSnapshot snapshot = validatePayload(event);
        Long deductionId = IdWorker.getId();
        repository.insertDeduction(
                deductionId,
                event.tenantId(),
                event.orgId(),
                event.eventId(),
                snapshot.deductionId(),
                snapshot.deductionNo(),
                snapshot.supplierClaimId(),
                snapshot.purchaseOrderId(),
                snapshot.businessPartnerId(),
                snapshot.businessPartnerCode(),
                snapshot.businessPartnerName(),
                snapshot.currencyCode(),
                snapshot.amount(),
                event.operatorId()
        );

        List<AllocationResult> allocations = allocate(
                deductionId, event, snapshot);

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
                event.businessDate() == null ? LocalDate.now() : event.businessDate(),
                event.sourceDocumentNo(),
                event.payload()
        );
        RuleEvaluation evaluation = ruleEngine.evaluate(
                ACCOUNTING_EVENT_TYPE, context);

        persistAccountingResult(accountingEventPk, evaluation);
        commonRepository.markAccountingReady(
                accountingEventPk, evaluation.rule(), sha256(factsJson));

        BizfiFiVoucher voucher = createOrReuseVoucherDraft(
                event, accountingEventId, evaluation);
        List<BizfiFiVoucherLine> voucherLines =
                voucherService.listLines(voucher.getFid());
        persistVoucherDimensions(
                voucher.getFid(), voucherLines, evaluation.lines());

        commonRepository.markAccountingVoucherGenerated(
                accountingEventPk, voucher.getFid(), voucher.getFnumber());
        repository.completeDeduction(
                deductionId, event.tenantId(), accountingEventId,
                voucher.getFid(), voucher.getFnumber(), event.operatorId());

        Set<Long> payableIds = allocations.stream()
                .map(AllocationResult::payableId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long payableId : payableIds) {
            repository.insertAccountingTrace(
                    IdWorker.getId(),
                    event.tenantId(),
                    event.orgId(),
                    event.eventId(),
                    event.eventType(),
                    event.sourceDocumentId(),
                    event.sourceDocumentNo(),
                    payableId,
                    deductionId,
                    accountingEventId,
                    evaluation.rule().ruleCode(),
                    evaluation.rule().versionNo(),
                    voucher.getFid(),
                    voucher.getFnumber()
            );
        }

        Long firstPayableId = payableIds.stream().findFirst().orElse(null);
        commonRepository.markInboxProcessed(
                consumerCode,
                event.eventId(),
                firstPayableId,
                accountingEventId,
                voucher.getFid(),
                voucher.getFnumber()
        );

        return new ProcessingResult(
                false, firstPayableId, accountingEventId,
                voucher.getFid(), voucher.getFnumber());
    }

    private List<AllocationResult> allocate(
            Long deductionId,
            BusinessEventEnvelope event,
            DeductionSnapshot snapshot
    ) {
        List<AllocationResult> results = new ArrayList<>();
        List<DeductionEntrySnapshot> entries = new ArrayList<>(snapshot.entries());
        entries.sort(Comparator
                .comparing(DeductionEntrySnapshot::purchaseOrderEntryId)
                .thenComparing(DeductionEntrySnapshot::deductionEntryId));

        for (DeductionEntrySnapshot deductionEntry : entries) {
            BigDecimal remaining = money(deductionEntry.amount());
            List<PayableCandidate> candidates =
                    repository.findFormalPayableCandidatesForUpdate(
                            event.tenantId(),
                            event.orgId(),
                            snapshot.businessPartnerId(),
                            snapshot.currencyCode(),
                            deductionEntry.purchaseOrderEntryId()
                    );
            Map<Long, BigDecimal> payableOpen = new HashMap<>();

            for (PayableCandidate candidate : candidates) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal currentOpen = payableOpen.computeIfAbsent(
                        candidate.payableId(), ignored -> money(candidate.openAmount()));
                BigDecimal unreserved = currentOpen.subtract(
                        money(candidate.reservedAmount()));
                BigDecimal lineAvailable = money(candidate.lineAmount())
                        .subtract(money(candidate.lineDeductedAmount()));
                BigDecimal available = minPositive(unreserved, lineAvailable);
                if (available.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal allocated = remaining.min(available);
                BigDecimal newOpen = currentOpen.subtract(allocated);
                if (newOpen.compareTo(money(candidate.reservedAmount())) < 0) {
                    throw new BizException(
                            "PURCHASE_DEDUCTION_RESERVED_AP_CONFLICT: "
                                    + candidate.payableNumber());
                }

                String nextStatus = newOpen.compareTo(BigDecimal.ZERO) == 0
                        ? "SETTLED" : "PARTIAL_SETTLED";
                repository.applyPayableDeduction(
                        candidate.payableId(),
                        event.tenantId(),
                        allocated,
                        newOpen,
                        nextStatus,
                        event.operatorId()
                );
                repository.insertAllocation(
                        IdWorker.getId(),
                        event.tenantId(),
                        event.orgId(),
                        deductionId,
                        event.eventId(),
                        deductionEntry.deductionEntryId(),
                        deductionEntry.purchaseOrderEntryId(),
                        candidate.payableId(),
                        candidate.payableEntryId(),
                        allocated,
                        currentOpen,
                        newOpen,
                        event.operatorId()
                );
                payableOpen.put(candidate.payableId(), newOpen);
                remaining = remaining.subtract(allocated);
                results.add(new AllocationResult(
                        deductionEntry.deductionEntryId(),
                        deductionEntry.purchaseOrderEntryId(),
                        candidate.payableId(),
                        candidate.payableEntryId(),
                        allocated
                ));
            }

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                throw new BizException(
                        "PURCHASE_DEDUCTION_FORMAL_AP_INSUFFICIENT: purchaseOrderEntryId="
                                + deductionEntry.purchaseOrderEntryId()
                                + ", remaining=" + remaining.toPlainString());
            }
        }

        BigDecimal allocatedTotal = results.stream()
                .map(AllocationResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (money(allocatedTotal).compareTo(snapshot.amount()) != 0) {
            throw new BizException("PURCHASE_DEDUCTION_ALLOCATION_TOTAL_MISMATCH");
        }
        return List.copyOf(results);
    }

    private DeductionSnapshot validatePayload(BusinessEventEnvelope event) {
        JsonNode payload = event.payload();
        if (payload == null || !payload.isObject()) {
            throw new BizException("PURCHASE_DEDUCTION payload 必须是对象");
        }
        String deductionId = requiredText(payload, "purchaseDeductionId");
        String deductionNo = requiredText(payload, "purchaseDeductionNo");
        String supplierClaimId = requiredText(payload, "supplierClaimId");
        String purchaseOrderId = requiredText(payload, "purchaseOrderId");
        String businessPartnerId = requiredText(payload, "businessPartnerId");
        String businessPartnerCode = nullableText(payload.get("businessPartnerCode"));
        String businessPartnerName = nullableText(payload.get("businessPartnerName"));
        String currencyCode = requiredText(payload, "currencyCode");
        BigDecimal amount = requiredMoney(payload, "amount");

        if (!deductionId.equals(event.sourceDocumentId())) {
            throw new BizException("PURCHASE_DEDUCTION sourceDocumentId 不一致");
        }
        if (event.sourceDocumentNo() != null
                && !event.sourceDocumentNo().equals(deductionNo)) {
            throw new BizException("PURCHASE_DEDUCTION sourceDocumentNo 不一致");
        }

        JsonNode array = payload.path("entries");
        if (!array.isArray() || array.isEmpty()) {
            throw new BizException("PURCHASE_DEDUCTION 缺少 entries");
        }
        List<DeductionEntrySnapshot> entries = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode item : array) {
            String entryId = requiredText(item, "purchaseDeductionEntryId");
            String poId = requiredText(item, "purchaseOrderId");
            String poEntryId = requiredText(item, "purchaseOrderEntryId");
            BigDecimal entryAmount = requiredMoney(item, "amount");
            if (!purchaseOrderId.equals(poId)) {
                throw new BizException("PURCHASE_DEDUCTION entry purchaseOrderId 不一致");
            }
            entries.add(new DeductionEntrySnapshot(
                    entryId, poEntryId, entryAmount));
            total = total.add(entryAmount);
        }
        if (money(total).compareTo(amount) != 0) {
            throw new BizException("PURCHASE_DEDUCTION entries 合计与 amount 不一致");
        }
        return new DeductionSnapshot(
                deductionId, deductionNo, supplierClaimId,
                purchaseOrderId, businessPartnerId,
                businessPartnerCode, businessPartnerName,
                currencyCode, amount, List.copyOf(entries));
    }

    private void validateEnvelope(BusinessEventEnvelope event) {
        if (!BUSINESS_EVENT_TYPE.equals(event.eventType())) {
            throw new BizException(
                    "UNSUPPORTED_BUSINESS_EVENT: " + event.eventType());
        }
        if (event.eventVersion() != 1) {
            throw new BizException(
                    "UNSUPPORTED_BUSINESS_EVENT_VERSION: " + event.eventVersion());
        }
        if (!"erp-service".equals(event.producerService())) {
            throw new BizException("PURCHASE_DEDUCTION producer must be erp-service");
        }
        if (!"ERP_PURCHASE_DEDUCTION".equals(event.sourceDocumentType())) {
            throw new BizException(
                    "PURCHASE_DEDUCTION sourceDocumentType must be ERP_PURCHASE_DEDUCTION");
        }
        if (event.orgId() == null) {
            throw new BizException("PURCHASE_DEDUCTION 缺少 orgId");
        }
        if (event.businessDate() == null) {
            throw new BizException("PURCHASE_DEDUCTION 缺少 businessDate");
        }
    }

    private void persistAccountingResult(
            Long accountingEventPk,
            RuleEvaluation evaluation
    ) {
        Map<Integer, Long> ids = evaluation.lines().stream()
                .collect(Collectors.toMap(
                        AccountingLine::lineNo,
                        ignored -> IdWorker.getId()));
        for (AccountingLine line : evaluation.lines()) {
            Long entryId = ids.get(line.lineNo());
            commonRepository.insertAccountingEntry(
                    entryId, accountingEventPk, line);
            for (var dimension : line.dimensions()) {
                commonRepository.insertAccountingDimension(
                        IdWorker.getId(), accountingEventPk, entryId,
                        dimension.code(), dimension.valueId(),
                        dimension.valueCode(), dimension.valueName());
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
                        .last("limit 1"));
        if (existing != null) {
            return existing;
        }

        BizfiFiVoucher voucher = new BizfiFiVoucher();
        voucher.setTenantId(event.tenantId());
        voucher.setOrganizationId(
                event.orgId() == null ? null : String.valueOf(event.orgId()));
        voucher.setBookId(defaultBookId);
        voucher.setSourceRequestId(sourceRequestId);
        voucher.setFdate(event.businessDate());
        voucher.setFsummary(
                "采购扣款-" + safe(event.sourceDocumentNo()));
        voucher.setFamount(evaluation.debitTotal());
        voucher.setFcreatedBy("accounting-engine");
        voucher.setFremark(
                "AccountingEvent=" + accountingEventId
                        + "; BusinessEvent=" + event.eventId());
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
                        (first, ignored) -> first));
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
                        dimension.valueName());
            }
        }
    }

    private String newAccountingEventId() {
        return "AE-DED-"
                + UUID.randomUUID().toString()
                .replace("-", "").toUpperCase();
    }

    private String requiredText(JsonNode node, String field) {
        String value = nullableText(node.get(field));
        if (value == null || value.isBlank()) {
            throw new BizException(
                    "PURCHASE_DEDUCTION 缺少字段: " + field);
        }
        return value;
    }

    private String nullableText(JsonNode node) {
        return node == null || node.isNull()
                ? null : node.asText();
    }

    private BigDecimal requiredMoney(JsonNode node, String field) {
        String value = nullableText(node.get(field));
        if (value == null || value.isBlank()) {
            throw new BizException(
                    "PURCHASE_DEDUCTION 缺少金额字段: " + field);
        }
        try {
            BigDecimal amount = money(new BigDecimal(value));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(
                        "PURCHASE_DEDUCTION 金额必须大于0: " + field);
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw new BizException(
                    "PURCHASE_DEDUCTION 金额格式错误: " + field);
        }
    }

    private BigDecimal minPositive(BigDecimal left, BigDecimal right) {
        BigDecimal value = left.min(right);
        return value.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO.setScale(2) : value;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BizException(
                    "PURCHASE_DEDUCTION accounting payload 序列化失败");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "SHA-256 unavailable", exception);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record DeductionSnapshot(
            String deductionId,
            String deductionNo,
            String supplierClaimId,
            String purchaseOrderId,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            List<DeductionEntrySnapshot> entries
    ) {}

    private record DeductionEntrySnapshot(
            String deductionEntryId,
            String purchaseOrderEntryId,
            BigDecimal amount
    ) {}

    private record AllocationResult(
            String deductionEntryId,
            String purchaseOrderEntryId,
            Long payableId,
            Long payableEntryId,
            BigDecimal amount
    ) {}
}
