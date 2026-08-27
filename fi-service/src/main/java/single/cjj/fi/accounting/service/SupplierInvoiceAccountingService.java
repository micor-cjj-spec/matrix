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
import single.cjj.fi.accounting.persistence.SupplierInvoiceAccountingRepository;
import single.cjj.fi.accounting.persistence.SupplierInvoiceAccountingRepository.EstimateEntry;
import single.cjj.fi.accounting.persistence.SupplierInvoiceAccountingRepository.OriginalAccountingEvent;
import single.cjj.fi.accounting.persistence.SupplierInvoiceAccountingRepository.PayableEntry;
import single.cjj.fi.accounting.persistence.SupplierInvoiceAccountingRepository.PayableHeader;
import single.cjj.fi.accounting.service.EstimateFullReversalPlanner.EstimateCandidate;
import single.cjj.fi.accounting.service.EstimateFullReversalPlanner.InvoiceLine;
import single.cjj.fi.accounting.service.EstimateFullReversalPlanner.Plan;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SupplierInvoiceAccountingService {

    public static final String BUSINESS_EVENT_TYPE = "SUPPLIER_INVOICE_CONFIRMED";
    public static final String FORMAL_ACCOUNTING_EVENT_TYPE = "PURCHASE_AP_RECOGNITION";
    public static final String REVERSAL_ACCOUNTING_EVENT_TYPE = "PURCHASE_ESTIMATE_REVERSAL";
    public static final String RESIDUAL_ACCOUNTING_EVENT_TYPE = "PURCHASE_RESIDUAL_ESTIMATE_RECOGNITION";
    private static final BigDecimal CENT = new BigDecimal("0.01");

    private final ObjectMapper objectMapper;
    private final InboundAccountingRepository commonRepository;
    private final SupplierInvoiceAccountingRepository repository;
    private final AccountingRuleEngine ruleEngine;
    private final BizfiFiVoucherService voucherService;
    private final BizfiFiVoucherMapper voucherMapper;
    private final EstimateFullReversalPlanner reversalPlanner = new EstimateFullReversalPlanner();
    private final EstimateSnapshotAccountingFactory snapshotFactory = new EstimateSnapshotAccountingFactory();
    private final String consumerCode;
    private final String defaultBookId;

    public SupplierInvoiceAccountingService(
            ObjectMapper objectMapper,
            InboundAccountingRepository commonRepository,
            SupplierInvoiceAccountingRepository repository,
            AccountingRuleEngine ruleEngine,
            BizfiFiVoucherService voucherService,
            BizfiFiVoucherMapper voucherMapper,
            @Value("${fi.accounting.supplier-invoice-consumer-code:FI_SUPPLIER_INVOICE_ACCOUNTING_V1}") String consumerCode,
            @Value("${fi.accounting.default-book-id:DEFAULT}") String defaultBookId
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

        PayloadSnapshot snapshot = validatePayload(event.payload());
        Long existingFormalPayable = commonRepository.findPayableIdByEvent(event.tenantId(), event.eventId());
        if (existingFormalPayable != null) {
            throw new BizException("FORMAL_AP_ALREADY_EXISTS_WITH_UNPROCESSED_INBOX: " + existingFormalPayable);
        }

        Map<String, List<EstimateEntry>> estimateEntriesByPoEntry = lockEstimateCandidates(event, snapshot);
        Plan plan = reversalPlanner.plan(
                snapshot.entries().stream()
                        .map(item -> new InvoiceLine(item.invoiceEntryId(), item.purchaseOrderEntryId(), item.quantity()))
                        .toList(),
                estimateEntriesByPoEntry.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        item -> item.getValue().stream().map(entry -> new EstimateCandidate(
                                entry.payableId(), entry.entryId(), entry.purchaseOrderEntryId(),
                                entry.quantity(), entry.unitPrice(), entry.amount()
                        )).toList(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ))
        );

        Long formalPayableId = createFormalPayable(event, snapshot);
        int reversalSequence = 1;
        int residualSequence = 1;

        for (Long estimatePayableId : plan.affectedPayableIds()) {
            PayableHeader estimate = requireReversibleEstimate(event, estimatePayableId);
            List<PayableEntry> estimateEntries = repository.findPayableEntries(estimatePayableId);
            if (estimateEntries.isEmpty()) {
                throw new BizException("ESTIMATE_PAYABLE_ENTRIES_EMPTY: " + estimatePayableId);
            }

            ResidualPlan residual = residualPlan(estimateEntries, plan);
            Long residualPayableId = null;
            if (residual.totalAmount().compareTo(BigDecimal.ZERO) > 0) {
                residualPayableId = createResidualEstimate(event, estimate, residual, snapshot.invoiceId());
            }

            Long reversalId = IdWorker.getId();
            repository.insertEstimateReversal(
                    reversalId,
                    event,
                    snapshot.invoiceId(),
                    event.sourceDocumentNo(),
                    estimatePayableId,
                    formalPayableId,
                    estimate.amount(),
                    residualPayableId,
                    residual.totalAmount(),
                    event.operatorId()
            );
            for (var allocation : plan.allocationsForPayable(estimatePayableId)) {
                repository.insertEstimateReversalAllocation(
                        IdWorker.getId(),
                        event,
                        reversalId,
                        allocation.invoiceEntryId(),
                        allocation.estimateEntryId(),
                        allocation.quantity(),
                        allocation.amount(),
                        event.operatorId()
                );
            }

            OriginalSnapshot original = loadOriginalSnapshot(estimate);
            AccountingArtifact reversalArtifact = createSnapshotAccounting(
                    event,
                    REVERSAL_ACCOUNTING_EVENT_TYPE,
                    reversalSequence++,
                    estimatePayableId,
                    snapshotFactory.fullReversal(original.lines(), original.dimensions()),
                    "REVERSAL_SNAPSHOT",
                    original.event().ruleVersion(),
                    reversalFacts(event, estimate, formalPayableId, residualPayableId, residual),
                    "采购暂估全额冲回-" + safe(estimate.number())
            );

            String residualAccountingEventId = null;
            if (residualPayableId != null) {
                List<AccountingLine> residualLines = snapshotFactory.residualRecognition(
                        original.lines(),
                        original.dimensions(),
                        residual.amountBySourceEntry(),
                        residual.totalAmount()
                );
                AccountingArtifact residualArtifact = createSnapshotAccounting(
                        event,
                        RESIDUAL_ACCOUNTING_EVENT_TYPE,
                        residualSequence++,
                        residualPayableId,
                        residualLines,
                        "RESIDUAL_FROM_ORIGINAL_SNAPSHOT",
                        original.event().ruleVersion(),
                        residualFacts(event, estimate, residualPayableId, residual),
                        "采购残余暂估-" + safe(estimate.number())
                );
                residualAccountingEventId = residualArtifact.accountingEventId();
                repository.updatePayableAccounting(
                        residualPayableId,
                        residualArtifact.accountingEventId(),
                        residualArtifact.voucherId(),
                        residualArtifact.voucherNumber()
                );
            }

            repository.markEstimateReversed(estimatePayableId, event.operatorId());
            repository.completeEstimateReversal(
                    reversalId,
                    reversalArtifact.accountingEventId(),
                    residualAccountingEventId,
                    event.operatorId()
            );
        }

        AccountingArtifact formalArtifact = createFormalAccounting(event, formalPayableId);
        repository.updatePayableAccounting(
                formalPayableId,
                formalArtifact.accountingEventId(),
                formalArtifact.voucherId(),
                formalArtifact.voucherNumber()
        );
        commonRepository.markInboxProcessed(
                consumerCode,
                event.eventId(),
                formalPayableId,
                formalArtifact.accountingEventId(),
                formalArtifact.voucherId(),
                formalArtifact.voucherNumber()
        );

        return new ProcessingResult(
                false,
                formalPayableId,
                formalArtifact.accountingEventId(),
                formalArtifact.voucherId(),
                formalArtifact.voucherNumber()
        );
    }

    private Map<String, List<EstimateEntry>> lockEstimateCandidates(
            BusinessEventEnvelope event,
            PayloadSnapshot snapshot
    ) {
        Map<String, List<EstimateEntry>> result = new LinkedHashMap<>();
        List<String> poEntryIds = snapshot.entries().stream()
                .map(InvoicePayloadEntry::purchaseOrderEntryId)
                .distinct()
                .sorted()
                .toList();
        for (String poEntryId : poEntryIds) {
            result.put(poEntryId, repository.findOpenEstimateEntriesForUpdate(
                    event.tenantId(),
                    event.orgId(),
                    snapshot.businessPartnerId(),
                    snapshot.currencyCode(),
                    poEntryId
            ));
        }
        return result;
    }

    private Long createFormalPayable(BusinessEventEnvelope event, PayloadSnapshot snapshot) {
        Long payableId = IdWorker.getId();
        repository.insertPayable(
                payableId,
                event.tenantId(),
                event.orgId(),
                buildFormalPayableNumber(event),
                "FORMAL",
                event.businessDate(),
                snapshot.businessPartnerId(),
                nullableText(event.payload().get("businessPartnerCode")),
                nullableText(event.payload().get("businessPartnerName")),
                snapshot.currencyCode(),
                snapshot.grossAmount(),
                event.sourceSystemCode(),
                event.sourceDocumentType(),
                event.sourceDocumentId(),
                event.sourceDocumentNo(),
                event.eventId(),
                null,
                event.operatorId()
        );

        int lineNo = 1;
        for (InvoicePayloadEntry entry : snapshot.entries()) {
            repository.insertPayableEntry(
                    IdWorker.getId(),
                    event.tenantId(),
                    event.orgId(),
                    payableId,
                    lineNo++,
                    entry.invoiceEntryId(),
                    entry.purchaseOrderId(),
                    entry.purchaseOrderEntryId(),
                    entry.materialId(),
                    entry.materialCode(),
                    entry.materialName(),
                    entry.quantity(),
                    entry.unitPrice(),
                    entry.grossAmount(),
                    entry.netAmount(),
                    entry.taxRate(),
                    entry.taxAmount(),
                    entry.grossAmount(),
                    null,
                    entry.projectId(),
                    entry.costCenterId(),
                    event.operatorId()
            );
        }
        return payableId;
    }

    private PayableHeader requireReversibleEstimate(BusinessEventEnvelope event, Long payableId) {
        PayableHeader estimate = repository.findPayableForUpdate(payableId, event.tenantId());
        if (estimate == null) {
            throw new BizException("ESTIMATE_PAYABLE_NOT_FOUND: " + payableId);
        }
        if (!Objects.equals(event.orgId(), estimate.orgId())) {
            throw new BizException("ESTIMATE_ORG_MISMATCH: " + payableId);
        }
        if (!"ESTIMATE".equals(estimate.type()) || !"OPEN".equals(estimate.status())) {
            throw new BizException("ESTIMATE_PAYABLE_NOT_REVERSIBLE: " + payableId);
        }
        if (!"VOUCHER_GENERATED".equals(estimate.accountingStatus())
                || estimate.accountingEventId() == null || estimate.accountingEventId().isBlank()) {
            throw new BizException("ESTIMATE_ACCOUNTING_NOT_READY_FOR_REVERSAL: " + payableId);
        }
        if (money(estimate.amount()).compareTo(money(estimate.openAmount())) != 0) {
            throw new BizException("ESTIMATE_NOT_FULLY_OPEN_FOR_REVERSAL: " + payableId
                    + ", amount=" + estimate.amount() + ", open=" + estimate.openAmount());
        }
        return estimate;
    }

    private ResidualPlan residualPlan(List<PayableEntry> entries, Plan plan) {
        List<ResidualEntry> residualEntries = new ArrayList<>();
        Map<String, BigDecimal> amountBySourceEntry = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (PayableEntry entry : entries) {
            var consumed = plan.consumption(entry.id());
            BigDecimal residualQuantity = nz(entry.quantity()).subtract(nz(consumed.quantity()));
            BigDecimal residualAmount = money(nz(entry.amount()).subtract(nz(consumed.amount())));
            if (residualQuantity.compareTo(BigDecimal.ZERO) < 0 || residualAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("RESIDUAL_ESTIMATE_NEGATIVE: estimateEntry=" + entry.id());
            }
            if (residualQuantity.compareTo(BigDecimal.ZERO) == 0) {
                if (residualAmount.abs().compareTo(CENT) >= 0) {
                    throw new BizException("RESIDUAL_ESTIMATE_ROUNDING_MISMATCH: estimateEntry=" + entry.id());
                }
                continue;
            }
            if (residualAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("RESIDUAL_ESTIMATE_AMOUNT_INVALID: estimateEntry=" + entry.id());
            }
            residualEntries.add(new ResidualEntry(entry, residualQuantity, residualAmount));
            amountBySourceEntry.merge(entry.sourceEntryId(), residualAmount, BigDecimal::add);
            total = total.add(residualAmount);
        }

        return new ResidualPlan(
                List.copyOf(residualEntries),
                Map.copyOf(amountBySourceEntry),
                money(total)
        );
    }

    private Long createResidualEstimate(
            BusinessEventEnvelope event,
            PayableHeader original,
            ResidualPlan residual,
            String supplierInvoiceId
    ) {
        Long payableId = IdWorker.getId();
        String residualBusinessEventId = residualBusinessEventId(event.eventId(), original.id());
        repository.insertPayable(
                payableId,
                event.tenantId(),
                event.orgId(),
                buildResidualPayableNumber(original, supplierInvoiceId),
                "ESTIMATE",
                event.businessDate(),
                original.businessPartnerId(),
                original.businessPartnerCode(),
                original.businessPartnerName(),
                original.currencyCode(),
                residual.totalAmount(),
                original.sourceSystemCode(),
                original.sourceDocumentType(),
                original.sourceDocumentId(),
                original.sourceDocumentNo(),
                residualBusinessEventId,
                original.id(),
                event.operatorId()
        );

        int lineNo = 1;
        for (ResidualEntry residualEntry : residual.entries()) {
            PayableEntry source = residualEntry.source();
            repository.insertPayableEntry(
                    IdWorker.getId(),
                    event.tenantId(),
                    event.orgId(),
                    payableId,
                    lineNo++,
                    source.sourceEntryId(),
                    source.purchaseOrderId(),
                    source.purchaseOrderEntryId(),
                    source.materialId(),
                    source.materialCode(),
                    source.materialName(),
                    residualEntry.quantity(),
                    source.unitPrice(),
                    residualEntry.amount(),
                    residualEntry.amount(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    residualEntry.amount(),
                    source.warehouseId(),
                    source.projectId(),
                    source.costCenterId(),
                    event.operatorId()
            );
        }
        return payableId;
    }

    private OriginalSnapshot loadOriginalSnapshot(PayableHeader estimate) {
        OriginalAccountingEvent originalEvent = repository.findOriginalAccountingEvent(estimate.accountingEventId());
        if (originalEvent == null) {
            throw new BizException("ORIGINAL_ACCOUNTING_EVENT_NOT_FOUND: " + estimate.accountingEventId());
        }
        var lines = repository.findOriginalAccountingLines(originalEvent.pk());
        if (lines.isEmpty()) {
            throw new BizException("ORIGINAL_ACCOUNTING_RESULT_EMPTY: " + estimate.accountingEventId());
        }
        return new OriginalSnapshot(
                originalEvent,
                lines,
                repository.findOriginalAccountingDimensions(originalEvent.pk())
        );
    }

    private AccountingArtifact createSnapshotAccounting(
            BusinessEventEnvelope event,
            String accountingEventType,
            int sequenceNo,
            Long payableId,
            List<AccountingLine> lines,
            String snapshotRuleCode,
            int originalRuleVersion,
            Object facts,
            String voucherSummary
    ) {
        if (lines == null || lines.isEmpty()) {
            throw new BizException("SNAPSHOT_ACCOUNTING_LINES_EMPTY: " + accountingEventType);
        }
        String factsJson = toJson(facts);
        Long accountingEventPk = IdWorker.getId();
        String accountingEventId = newAccountingEventId();
        repository.insertSnapshotAccountingEvent(
                accountingEventPk,
                accountingEventId,
                accountingEventType,
                sequenceNo,
                event,
                defaultBookId,
                factsJson,
                snapshotRuleCode,
                originalRuleVersion,
                sha256(factsJson)
        );
        persistAccountingLines(accountingEventPk, lines);
        BizfiFiVoucher voucher = createOrReuseVoucherDraft(
                event,
                accountingEventId,
                lines,
                voucherSummary
        );
        persistVoucherDimensions(voucher.getFid(), voucherService.listLines(voucher.getFid()), lines);
        commonRepository.markAccountingVoucherGenerated(
                accountingEventPk, voucher.getFid(), voucher.getFnumber());
        commonRepository.insertTrace(
                IdWorker.getId(),
                event,
                payableId,
                accountingEventId,
                snapshotRuleCode,
                originalRuleVersion,
                voucher.getFid(),
                voucher.getFnumber()
        );
        return new AccountingArtifact(accountingEventId, voucher.getFid(), voucher.getFnumber());
    }

    private AccountingArtifact createFormalAccounting(BusinessEventEnvelope event, Long formalPayableId) {
        Long accountingEventPk = IdWorker.getId();
        String accountingEventId = newAccountingEventId();
        String factsJson = toJson(event.payload());
        commonRepository.insertAccountingEvent(
                accountingEventPk,
                accountingEventId,
                FORMAL_ACCOUNTING_EVENT_TYPE,
                event,
                defaultBookId,
                factsJson
        );

        EventContext context = new EventContext(
                event.tenantId(),
                event.orgId(),
                defaultBookId,
                event.businessDate(),
                event.sourceDocumentNo(),
                event.payload()
        );
        RuleEvaluation evaluation = ruleEngine.evaluate(FORMAL_ACCOUNTING_EVENT_TYPE, context);
        persistAccountingLines(accountingEventPk, evaluation.lines());
        commonRepository.markAccountingReady(
                accountingEventPk, evaluation.rule(), sha256(factsJson));

        BizfiFiVoucher voucher = createOrReuseVoucherDraft(
                event,
                accountingEventId,
                evaluation.lines(),
                "采购发票正式应付-" + safe(event.sourceDocumentNo())
        );
        persistVoucherDimensions(
                voucher.getFid(),
                voucherService.listLines(voucher.getFid()),
                evaluation.lines()
        );
        commonRepository.markAccountingVoucherGenerated(
                accountingEventPk, voucher.getFid(), voucher.getFnumber());
        commonRepository.insertTrace(
                IdWorker.getId(),
                event,
                formalPayableId,
                accountingEventId,
                evaluation.rule().ruleCode(),
                evaluation.rule().versionNo(),
                voucher.getFid(),
                voucher.getFnumber()
        );
        return new AccountingArtifact(accountingEventId, voucher.getFid(), voucher.getFnumber());
    }

    private void persistAccountingLines(Long accountingEventPk, List<AccountingLine> lines) {
        for (AccountingLine line : lines) {
            Long entryId = IdWorker.getId();
            commonRepository.insertAccountingEntry(entryId, accountingEventPk, line);
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
            List<AccountingLine> accountingLines,
            String summary
    ) {
        String sourceRequestId = "ACCOUNTING:" + accountingEventId + ":VOUCHER:0";
        BizfiFiVoucher existing = voucherMapper.selectOne(new LambdaQueryWrapper<BizfiFiVoucher>()
                .eq(BizfiFiVoucher::getTenantId, event.tenantId())
                .eq(BizfiFiVoucher::getSourceRequestId, sourceRequestId)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }

        BigDecimal debitTotal = accountingLines.stream()
                .map(AccountingLine::debitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal creditTotal = accountingLines.stream()
                .map(AccountingLine::creditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new BizException("ACCOUNTING_UNBALANCED_BEFORE_VOUCHER: debit="
                    + debitTotal + ", credit=" + creditTotal);
        }

        BizfiFiVoucher voucher = new BizfiFiVoucher();
        voucher.setTenantId(event.tenantId());
        voucher.setOrganizationId(event.orgId() == null ? null : String.valueOf(event.orgId()));
        voucher.setBookId(defaultBookId);
        voucher.setSourceRequestId(sourceRequestId);
        voucher.setFdate(event.businessDate());
        voucher.setFsummary(summary);
        voucher.setFamount(debitTotal);
        voucher.setFcreatedBy("accounting-engine");
        voucher.setFremark("AccountingEvent=" + accountingEventId + "; BusinessEvent=" + event.eventId());
        voucherService.saveDraft(voucher);

        List<BizfiFiVoucherLine> voucherLines = new ArrayList<>();
        for (AccountingLine accountingLine : accountingLines) {
            BizfiFiVoucherLine line = new BizfiFiVoucherLine();
            line.setFlineNo(accountingLine.lineNo());
            line.setFaccountCode(accountingLine.accountCode());
            line.setFsummary(accountingLine.summary());
            line.setFdebitAmount(accountingLine.debitAmount());
            line.setFcreditAmount(accountingLine.creditAmount());
            line.setFcurrency(accountingLine.currencyCode());
            line.setFrate(BigDecimal.ONE);
            line.setForiginalAmount(accountingLine.originalAmount());
            voucherLines.add(line);
        }
        voucherService.saveLines(voucher.getFid(), voucherLines);
        return voucherService.get(voucher.getFid());
    }

    private void persistVoucherDimensions(
            Long voucherId,
            List<BizfiFiVoucherLine> voucherLines,
            List<AccountingLine> accountingLines
    ) {
        Map<Integer, BizfiFiVoucherLine> byLineNo = voucherLines.stream()
                .collect(Collectors.toMap(
                        BizfiFiVoucherLine::getFlineNo,
                        Function.identity(),
                        (a, b) -> a
                ));
        for (AccountingLine accountingLine : accountingLines) {
            BizfiFiVoucherLine voucherLine = byLineNo.get(accountingLine.lineNo());
            if (voucherLine == null) {
                throw new BizException("VOUCHER_LINE_NOT_FOUND: " + accountingLine.lineNo());
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

    private PayloadSnapshot validatePayload(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new BizException("BUSINESS_EVENT_PAYLOAD_INVALID");
        }
        String invoiceId = requiredText(payload, "supplierInvoiceId");
        String partnerId = requiredText(payload, "businessPartnerId");
        String currencyCode = requiredText(payload, "currencyCode");
        BigDecimal netTotal = money(decimal(payload.get("netAmount"), "payload.netAmount"));
        BigDecimal taxTotal = money(decimal(payload.get("taxAmount"), "payload.taxAmount"));
        BigDecimal grossTotal = money(decimal(payload.get("grossAmount"), "payload.grossAmount"));
        if (netTotal.compareTo(BigDecimal.ZERO) <= 0 || taxTotal.compareTo(BigDecimal.ZERO) < 0
                || grossTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("SUPPLIER_INVOICE_AMOUNT_INVALID");
        }
        if (money(netTotal.add(taxTotal)).compareTo(grossTotal) != 0) {
            throw new BizException("SUPPLIER_INVOICE_HEADER_AMOUNT_MISMATCH");
        }

        JsonNode entriesNode = payload.path("entries");
        if (!entriesNode.isArray() || entriesNode.isEmpty()) {
            throw new BizException("SUPPLIER_INVOICE_ENTRIES_EMPTY");
        }

        List<InvoicePayloadEntry> entries = new ArrayList<>();
        BigDecimal netSum = BigDecimal.ZERO;
        BigDecimal taxSum = BigDecimal.ZERO;
        BigDecimal grossSum = BigDecimal.ZERO;
        for (JsonNode entry : entriesNode) {
            BigDecimal quantity = decimal(entry.get("quantity"), "entry.quantity");
            BigDecimal unitPrice = decimal(entry.get("unitPrice"), "entry.unitPrice");
            BigDecimal netAmount = money(decimal(entry.get("netAmount"), "entry.netAmount"));
            BigDecimal taxRate = decimal(entry.get("taxRate"), "entry.taxRate");
            BigDecimal taxAmount = money(decimal(entry.get("taxAmount"), "entry.taxAmount"));
            BigDecimal grossAmount = money(decimal(entry.get("grossAmount"), "entry.grossAmount"));
            if (quantity.compareTo(BigDecimal.ZERO) <= 0 || unitPrice.compareTo(BigDecimal.ZERO) < 0
                    || netAmount.compareTo(BigDecimal.ZERO) <= 0 || taxRate.compareTo(BigDecimal.ZERO) < 0
                    || taxAmount.compareTo(BigDecimal.ZERO) < 0 || grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("SUPPLIER_INVOICE_ENTRY_AMOUNT_INVALID");
            }
            if (money(netAmount.add(taxAmount)).compareTo(grossAmount) != 0) {
                throw new BizException("SUPPLIER_INVOICE_ENTRY_AMOUNT_MISMATCH: "
                        + requiredText(entry, "supplierInvoiceEntryId"));
            }

            entries.add(new InvoicePayloadEntry(
                    requiredText(entry, "supplierInvoiceEntryId"),
                    requiredText(entry, "purchaseOrderId"),
                    requiredText(entry, "purchaseOrderEntryId"),
                    requiredText(entry, "materialId"),
                    nullableText(entry.get("materialCode")),
                    nullableText(entry.get("materialName")),
                    nullableText(entry.get("specification")),
                    quantity,
                    unitPrice,
                    netAmount,
                    taxRate,
                    taxAmount,
                    grossAmount,
                    nullableText(entry.get("projectId")),
                    nullableText(entry.get("costCenterId"))
            ));
            netSum = netSum.add(netAmount);
            taxSum = taxSum.add(taxAmount);
            grossSum = grossSum.add(grossAmount);
        }
        if (money(netSum).subtract(netTotal).abs().compareTo(CENT) >= 0
                || money(taxSum).subtract(taxTotal).abs().compareTo(CENT) >= 0
                || money(grossSum).subtract(grossTotal).abs().compareTo(CENT) >= 0) {
            throw new BizException("SUPPLIER_INVOICE_ENTRY_TOTAL_MISMATCH");
        }

        return new PayloadSnapshot(
                invoiceId,
                partnerId,
                currencyCode,
                netTotal,
                taxTotal,
                grossTotal,
                List.copyOf(entries)
        );
    }

    private void validateEnvelope(BusinessEventEnvelope event) {
        if (!BUSINESS_EVENT_TYPE.equals(event.eventType())) {
            throw new BizException("UNSUPPORTED_BUSINESS_EVENT: " + event.eventType());
        }
        if (event.eventVersion() != 1) {
            throw new BizException("UNSUPPORTED_BUSINESS_EVENT_VERSION: " + event.eventVersion());
        }
        if (!"ERP_SUPPLIER_INVOICE".equals(event.sourceDocumentType())) {
            throw new BizException("UNSUPPORTED_SOURCE_DOCUMENT: " + event.sourceDocumentType());
        }
        if (event.tenantId() == null || event.tenantId().isBlank()) {
            throw new BizException("BUSINESS_EVENT_TENANT_MISSING");
        }
        if (event.orgId() == null) {
            throw new BizException("BUSINESS_EVENT_ORG_MISSING");
        }
        if (event.businessDate() == null) {
            throw new BizException("BUSINESS_EVENT_BUSINESS_DATE_MISSING");
        }
    }

    private Object reversalFacts(
            BusinessEventEnvelope event,
            PayableHeader original,
            Long formalPayableId,
            Long residualPayableId,
            ResidualPlan residual
    ) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("supplierInvoiceEventId", event.eventId());
        facts.put("supplierInvoiceId", event.sourceDocumentId());
        facts.put("supplierInvoiceNo", event.sourceDocumentNo());
        facts.put("originalEstimatePayableId", original.id());
        facts.put("originalEstimateAccountingEventId", original.accountingEventId());
        facts.put("formalPayableId", formalPayableId);
        facts.put("fullReversalAmount", original.amount());
        facts.put("residualPayableId", residualPayableId);
        facts.put("residualAmount", residual.totalAmount());
        return facts;
    }

    private Object residualFacts(
            BusinessEventEnvelope event,
            PayableHeader original,
            Long residualPayableId,
            ResidualPlan residual
    ) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("supplierInvoiceEventId", event.eventId());
        facts.put("supplierInvoiceId", event.sourceDocumentId());
        facts.put("originalEstimatePayableId", original.id());
        facts.put("originalEstimateAccountingEventId", original.accountingEventId());
        facts.put("residualPayableId", residualPayableId);
        facts.put("residualAmount", residual.totalAmount());
        facts.put("residualEntries", residual.entries().stream().map(item -> Map.of(
                "sourceEntryId", safe(item.source().sourceEntryId()),
                "purchaseOrderEntryId", safe(item.source().purchaseOrderEntryId()),
                "quantity", item.quantity(),
                "amount", item.amount()
        )).toList());
        return facts;
    }

    private String buildFormalPayableNumber(BusinessEventEnvelope event) {
        String source = safe(event.sourceDocumentNo());
        if (source.isBlank()) {
            source = safe(event.sourceDocumentId());
        }
        return truncate("AP-" + source, 100);
    }

    private String buildResidualPayableNumber(PayableHeader original, String supplierInvoiceId) {
        return truncate("APEST-R-" + original.id() + "-" + safe(supplierInvoiceId), 100);
    }

    private String residualBusinessEventId(String supplierInvoiceEventId, Long originalPayableId) {
        return truncate(supplierInvoiceEventId + ":RESIDUAL:" + originalPayableId, 100);
    }

    private String newAccountingEventId() {
        return "AE-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new BizException("SOURCE_PAYLOAD_HASH_FAILED");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
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

    private BigDecimal money(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record PayloadSnapshot(
            String invoiceId,
            String businessPartnerId,
            String currencyCode,
            BigDecimal netAmount,
            BigDecimal taxAmount,
            BigDecimal grossAmount,
            List<InvoicePayloadEntry> entries
    ) {
    }

    private record InvoicePayloadEntry(
            String invoiceEntryId,
            String purchaseOrderId,
            String purchaseOrderEntryId,
            String materialId,
            String materialCode,
            String materialName,
            String specification,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal netAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal grossAmount,
            String projectId,
            String costCenterId
    ) {
    }

    private record ResidualEntry(
            PayableEntry source,
            BigDecimal quantity,
            BigDecimal amount
    ) {
    }

    private record ResidualPlan(
            List<ResidualEntry> entries,
            Map<String, BigDecimal> amountBySourceEntry,
            BigDecimal totalAmount
    ) {
    }

    private record OriginalSnapshot(
            OriginalAccountingEvent event,
            List<SupplierInvoiceAccountingRepository.OriginalAccountingLine> lines,
            List<SupplierInvoiceAccountingRepository.OriginalDimension> dimensions
    ) {
    }

    private record AccountingArtifact(
            String accountingEventId,
            Long voucherId,
            String voucherNumber
    ) {
    }
}
