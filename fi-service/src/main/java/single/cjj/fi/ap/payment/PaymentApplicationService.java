package single.cjj.fi.ap.payment;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.ActionRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.AllocationRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.AllocationView;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.BotpCreateRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.BotpDocument;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.BudgetCheckRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.CreateRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.Detail;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.EvidenceRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.EvidenceVerifyRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.EvidenceView;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.FormalPayableView;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.PayableSnapshot;
import single.cjj.fi.ap.payment.PaymentApplicationRepository.AllocationRow;
import single.cjj.fi.ap.payment.PaymentApplicationRepository.ApplicationRow;
import single.cjj.fi.ap.payment.PaymentApplicationRepository.EvidenceRow;
import single.cjj.fi.ap.payment.PaymentApplicationRepository.PayableRow;
import single.cjj.fi.event.FiBusinessEventOutboxService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PaymentApplicationService {

    public static final String EVENT_APPROVED = "PAYMENT_APPLICATION_APPROVED";
    public static final String ROUTING_APPROVED = "biz.finance.payment_application.approved";

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_AUDITED = "AUDITED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    private static final String ALLOCATION_RESERVED = "RESERVED";
    private static final String ALLOCATION_RELEASED = "RELEASED";

    private static final String EVIDENCE_PENDING = "PENDING";
    private static final String EVIDENCE_VERIFIED = "VERIFIED";
    private static final String EVIDENCE_REJECTED = "REJECTED";

    private final PaymentApplicationRepository repository;
    private final FiBusinessEventOutboxService outboxService;
    private final ObjectMapper objectMapper;

    public PaymentApplicationService(
            PaymentApplicationRepository repository,
            FiBusinessEventOutboxService outboxService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail create(CreateRequest request, Long operatorId) {
        Long id = IdWorker.getId();
        Map<Long, BigDecimal> allocations = normalizeAllocations(request.allocations());
        List<PayableRow> payables = reservePayables(
                request.tenantId(), request.orgId(), allocations, operatorId);
        PayableRow first = payables.get(0);
        BigDecimal total = allocations.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        LocalDate date = request.applicationDate() == null ? LocalDate.now() : request.applicationDate();
        LocalDateTime now = LocalDateTime.now();

        ApplicationRow row = new ApplicationRow(
                id, request.tenantId(), request.orgId(), number(id, date), date,
                request.requesterId(),
                first.businessPartnerId(), first.businessPartnerCode(), first.businessPartnerName(),
                first.currencyCode(), total, request.fundPlanId(), request.plannedPayDate(),
                normalizeText(request.paymentMethod()),
                normalizeText(request.payeeBankAccountId()),
                normalizeText(request.payeeAccountName()),
                normalizeText(request.payeeBankName()),
                normalizeText(request.payeeBankAccountNo()),
                EVIDENCE_PENDING, "PENDING", null, null, null,
                STATUS_DRAFT, APPROVAL_DRAFT, "NOT_EXECUTED",
                null, null, null, null, null,
                normalizeText(request.remark()), null, null, null,
                operatorId, now, 0
        );
        repository.insertApplication(row);
        insertAllocations(id, request.tenantId(), request.orgId(), allocations, operatorId);
        insertEvidence(id, request.tenantId(), request.orgId(), request.evidence(), operatorId);
        refreshEvidenceStatus(id, request.tenantId(), operatorId);
        return detail(id, request.tenantId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail createFromBotp(BotpCreateRequest request) {
        ApplicationRow existing = repository.findByIdempotency(request.tenantId(), request.idempotencyKey());
        if (existing != null) {
            return detail(existing.id(), request.tenantId());
        }

        PayableRow source = repository.lockPayable(request.payableId(), request.tenantId());
        validatePayable(source, request.orgId());
        existing = repository.findByIdempotency(request.tenantId(), request.idempotencyKey());
        if (existing != null) {
            return detail(existing.id(), request.tenantId());
        }
        BigDecimal amount = money(request.amount());
        BigDecimal available = available(source);
        if (amount.compareTo(available) > 0) {
            throw new BizException("付款申请金额超过应付可用余额，当前可用: "
                    + available.stripTrailingZeros().toPlainString());
        }

        Long id = IdWorker.getId();
        LocalDate date = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        repository.setPayableReserved(
                source.id(), source.tenantId(), money(nz(source.reservedAmount()).add(amount)), request.operatorId());

        ApplicationRow row = new ApplicationRow(
                id, request.tenantId(), request.orgId(), number(id, date), date,
                request.operatorId(),
                source.businessPartnerId(), source.businessPartnerCode(), source.businessPartnerName(),
                source.currencyCode(), amount, null, request.plannedPayDate(), normalizeText(request.payMethod()),
                null, null, null, null,
                EVIDENCE_PENDING, "PENDING", null, null, null,
                STATUS_DRAFT, APPROVAL_DRAFT, "NOT_EXECUTED",
                request.idempotencyKey(), request.sourceSystem(), request.sourceDocumentType(),
                request.sourceDocumentId(), request.sourceExecutionId(),
                "BOTP来源: " + source.number(), null, null, null,
                request.operatorId(), now, 0
        );
        repository.insertApplication(row);
        repository.insertAllocation(new AllocationRow(
                IdWorker.getId(), request.tenantId(), request.orgId(), id,
                source.id(), source.number(), amount, amount,
                BigDecimal.ZERO.setScale(2), ALLOCATION_RESERVED,
                request.operatorId(), now, 0
        ));
        return detail(id, request.tenantId());
    }

    public Detail detail(Long fid, String tenantId) {
        ApplicationRow app = requireApplication(fid, tenantId, false);
        List<AllocationView> allocations = repository.findAllocations(fid, tenantId).stream()
                .map(item -> new AllocationView(
                        item.id(), item.payableId(), item.payableNumber(),
                        item.appliedAmount(), item.reservedAmount(),
                        nz(item.consumedAmount()),
                        money(nz(item.reservedAmount()).subtract(nz(item.consumedAmount()))),
                        item.status()))
                .toList();
        List<EvidenceView> evidence = repository.findEvidence(fid, tenantId).stream()
                .map(item -> new EvidenceView(
                        item.id(), item.evidenceType(), item.sourceSystemCode(),
                        item.sourceDocumentType(), item.sourceDocumentId(), item.sourceDocumentNo(),
                        item.required(), item.verificationStatus(), item.remark()))
                .toList();
        return new Detail(
                app.id(), app.tenantId(), app.orgId(), app.number(), app.date(),
                app.requesterId(), app.businessPartnerId(), app.businessPartnerCode(),
                app.businessPartnerName(), app.currencyCode(), app.amount(),
                app.fundPlanId(), app.plannedPayDate(), app.paymentMethod(),
                app.payeeBankAccountId(), app.payeeAccountName(), app.payeeBankName(),
                app.payeeBankAccountNo(), app.evidenceCheckStatus(), app.budgetCheckStatus(),
                app.budgetCheckId(), app.budgetAvailableAmount(), app.budgetCheckMessage(),
                app.status(), app.approvalStatus(), app.executionStatus(),
                app.botpIdempotencyKey(), app.sourceSystemCode(), app.sourceDocumentType(),
                app.sourceDocumentId(), app.sourceExecutionId(), app.remark(),
                app.rejectReason(), app.approvedBy(), app.approvedTime(), app.version(),
                allocations, evidence
        );
    }

    public List<Detail> list(String tenantId, Long orgId, String status, int limit) {
        return repository.listApplications(tenantId, orgId, status, limit).stream()
                .map(item -> detail(item.id(), tenantId))
                .toList();
    }

    public List<FormalPayableView> listFormalPayables(
            String tenantId,
            Long orgId,
            String status,
            int limit
    ) {
        return repository.listFormalPayables(tenantId, orgId, status, limit)
                .stream()
                .map(row -> new FormalPayableView(
                        row.id(),
                        row.tenantId(),
                        row.orgId(),
                        row.number(),
                        row.date(),
                        row.businessPartnerId(),
                        row.businessPartnerCode(),
                        row.businessPartnerName(),
                        row.currencyCode(),
                        row.amount(),
                        row.openAmount(),
                        row.settledAmount(),
                        nz(row.reservedAmount()),
                        money(nz(row.openAmount()).subtract(nz(row.reservedAmount()))),
                        row.status(),
                        row.approvalStatus(),
                        row.accountingStatus(),
                        row.sourceDocumentType(),
                        row.sourceDocumentId(),
                        row.sourceDocumentNo(),
                        row.accountingEventId(),
                        row.voucherId(),
                        row.voucherNumber(),
                        row.version()
                ))
                .toList();
    }

    public PayableSnapshot payable(Long fid, String tenantId) {
        PayableRow row = repository.findPayable(fid, tenantId);
        if (row == null) {
            throw new BizException("应付单不存在: " + fid);
        }
        return new PayableSnapshot(
                row.id(), row.tenantId(), row.orgId(), row.number(), row.type(), row.date(),
                row.businessPartnerId(), row.businessPartnerCode(), row.businessPartnerName(),
                row.currencyCode(), row.amount(), row.openAmount(), nz(row.reservedAmount()),
                available(row), row.status(), row.approvalStatus(), row.accountingStatus(), row.version()
        );
    }

    public Detail findByIdempotency(String tenantId, String idempotencyKey) {
        ApplicationRow row = repository.findByIdempotency(tenantId, idempotencyKey);
        return row == null ? null : detail(row.id(), tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail addEvidence(
            Long applicationId,
            String tenantId,
            EvidenceRequest request,
            Long operatorId
    ) {
        ApplicationRow app = requireEditable(applicationId, tenantId);
        repository.insertEvidence(toEvidence(
                IdWorker.getId(), app.tenantId(), app.orgId(), app.id(),
                request, operatorId, LocalDateTime.now()));
        refreshEvidenceStatus(applicationId, tenantId, operatorId);
        return detail(applicationId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail verifyEvidence(
            Long applicationId,
            Long evidenceId,
            String tenantId,
            EvidenceVerifyRequest request,
            Long operatorId
    ) {
        requireEditable(applicationId, tenantId);
        String status = normalizeEvidenceStatus(request.verificationStatus());
        repository.verifyEvidence(
                evidenceId, applicationId, tenantId, status,
                normalizeText(request.remark()), operatorId);
        refreshEvidenceStatus(applicationId, tenantId, operatorId);
        return detail(applicationId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail recordBudgetCheck(
            Long applicationId,
            String tenantId,
            BudgetCheckRequest request,
            Long operatorId
    ) {
        ApplicationRow app = requireEditable(applicationId, tenantId);
        String status = normalizeBudgetStatus(request.status());
        if ("PASSED".equals(status)) {
            if (request.availableAmount() == null) {
                throw new BizException("预算校验通过时必须返回可用额度");
            }
            if (money(request.availableAmount()).compareTo(money(app.amount())) < 0) {
                throw new BizException("预算可用额度小于付款申请金额，不能标记为 PASSED");
            }
        }
        repository.updateBudgetCheck(
                applicationId, tenantId, status, normalizeText(request.checkId()),
                normalizeText(request.fundPlanId()), request.availableAmount(),
                normalizeText(request.message()), jsonOrNull(request.snapshot()), operatorId);
        return detail(applicationId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail submit(Long applicationId, String tenantId, ActionRequest request) {
        Long operatorId = request == null ? null : request.operatorId();
        ApplicationRow app = requireApplication(applicationId, tenantId, true);
        if (!STATUS_DRAFT.equals(app.status()) && !STATUS_REJECTED.equals(app.status())) {
            throw new BizException("只有草稿或驳回状态付款申请可提交");
        }
        if (STATUS_REJECTED.equals(app.status())) {
            reReserve(applicationId, tenantId, operatorId);
            app = requireApplication(applicationId, tenantId, true);
        }
        validateControls(app, repository.findAllocations(applicationId, tenantId));
        repository.updateApplicationState(
                applicationId, tenantId, STATUS_SUBMITTED, APPROVAL_SUBMITTED,
                null, null, null, operatorId);
        return detail(applicationId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail approve(Long applicationId, String tenantId, ActionRequest request) {
        Long operatorId = request == null ? null : request.operatorId();
        ApplicationRow app = requireApplication(applicationId, tenantId, true);
        if (!STATUS_SUBMITTED.equals(app.status()) || !APPROVAL_SUBMITTED.equals(app.approvalStatus())) {
            throw new BizException("只有已提交付款申请可审批通过");
        }
        List<AllocationRow> allocations = repository.findAllocations(applicationId, tenantId);
        validateControls(app, allocations);
        validateReservations(allocations);

        LocalDateTime approvedTime = LocalDateTime.now();
        repository.updateApplicationState(
                applicationId, tenantId, STATUS_APPROVED, APPROVAL_AUDITED,
                null, operatorId, approvedTime, operatorId);
        ApplicationRow approved = requireApplication(applicationId, tenantId, false);
        outboxService.append(
                tenantId,
                approved.orgId(),
                EVENT_APPROVED,
                "AP",
                "PAYMENT_APPLICATION",
                approved.id(),
                approved.version() == null ? 0L : approved.version().longValue(),
                "FI_PAYMENT_APPLICATION",
                approved.number(),
                approved.date(),
                approved.botpIdempotencyKey(),
                approved.sourceExecutionId(),
                null,
                operatorId,
                ROUTING_APPROVED,
                approvedPayload(approved, allocations)
        );
        return detail(applicationId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail reject(Long applicationId, String tenantId, ActionRequest request) {
        Long operatorId = request == null ? null : request.operatorId();
        ApplicationRow app = requireApplication(applicationId, tenantId, true);
        if (!STATUS_SUBMITTED.equals(app.status())) {
            throw new BizException("只有已提交付款申请可驳回");
        }
        releaseReservations(applicationId, tenantId, operatorId);
        repository.updateApplicationState(
                applicationId, tenantId, STATUS_REJECTED, APPROVAL_REJECTED,
                normalizeText(request == null ? null : request.reason()),
                null, null, operatorId);
        return detail(applicationId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail cancel(Long applicationId, String tenantId, ActionRequest request) {
        Long operatorId = request == null ? null : request.operatorId();
        ApplicationRow app = requireApplication(applicationId, tenantId, true);
        if (!STATUS_DRAFT.equals(app.status()) && !STATUS_REJECTED.equals(app.status())) {
            throw new BizException("只有草稿或驳回状态付款申请可取消");
        }
        releaseReservations(applicationId, tenantId, operatorId);
        repository.updateApplicationState(
                applicationId, tenantId, STATUS_CANCELLED, app.approvalStatus(),
                normalizeText(request == null ? null : request.reason()),
                null, null, operatorId);
        return detail(applicationId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PayableSnapshot recomputePayableReservation(
            Long payableId,
            String tenantId,
            Long operatorId
    ) {
        PayableRow payable = repository.lockPayable(payableId, tenantId);
        if (payable == null) {
            throw new BizException("应付单不存在: " + payableId);
        }
        BigDecimal reserved = money(repository.sumReservedByPayable(payableId, tenantId));
        if (reserved.compareTo(money(payable.openAmount())) > 0) {
            throw new BizException("付款申请占用超过应付未核销余额: " + payableId);
        }
        repository.setPayableReserved(payableId, tenantId, reserved, operatorId);
        return payable(payableId, tenantId);
    }

    public BotpDocument botpPayable(Long payableId) {
        PayableRow row = repository.findPayableById(payableId);
        if (row == null) {
            throw new BizException("应付单不存在: " + payableId);
        }
        return new BotpDocument(
                "AP:" + row.id(), row.id(), row.number(), row.date(),
                row.tenantId(), row.orgId(), row.businessPartnerId(),
                row.businessPartnerCode(), row.businessPartnerName(),
                row.currencyCode(), row.amount(), row.openAmount(),
                nz(row.reservedAmount()), available(row), row.status(),
                row.approvalStatus(), row.accountingStatus(),
                null, null, null, null, null, null, row.version()
        );
    }

    public BotpDocument botpApplication(Long applicationId) {
        ApplicationRow app = repository.findApplicationById(applicationId);
        if (app == null) {
            throw new BizException("付款申请不存在: " + applicationId);
        }
        return new BotpDocument(
                "PA:" + app.id(), app.id(), app.number(), app.date(),
                app.tenantId(), app.orgId(), app.businessPartnerId(),
                app.businessPartnerCode(), app.businessPartnerName(), app.currencyCode(),
                app.amount(), null, null, null, app.status(), app.approvalStatus(), null,
                app.paymentMethod(), app.plannedPayDate(), app.sourceDocumentType(),
                app.sourceDocumentId(), app.sourceExecutionId(), app.botpIdempotencyKey(),
                app.version()
        );
    }

    private List<PayableRow> reservePayables(
            String tenantId,
            Long orgId,
            Map<Long, BigDecimal> allocations,
            Long operatorId
    ) {
        List<PayableRow> locked = new ArrayList<>();
        PayableRow reference = null;
        for (Long payableId : allocations.keySet().stream().sorted().toList()) {
            PayableRow payable = repository.lockPayable(payableId, tenantId);
            validatePayable(payable, orgId);
            if (reference == null) {
                reference = payable;
            } else {
                if (!Objects.equals(reference.businessPartnerId(), payable.businessPartnerId())) {
                    throw new BizException("一张付款申请只能包含同一供应商的应付单");
                }
                if (!Objects.equals(reference.currencyCode(), payable.currencyCode())) {
                    throw new BizException("一张付款申请只能包含同一币种的应付单");
                }
            }
            BigDecimal requested = allocations.get(payableId);
            BigDecimal available = available(payable);
            if (requested.compareTo(available) > 0) {
                throw new BizException("应付单 " + payable.number() + " 可申请余额不足，当前可用: "
                        + available.stripTrailingZeros().toPlainString());
            }
            locked.add(payable);
        }
        for (PayableRow payable : locked) {
            BigDecimal newReserved = money(
                    nz(payable.reservedAmount()).add(allocations.get(payable.id())));
            repository.setPayableReserved(
                    payable.id(), tenantId, newReserved, operatorId);
        }
        return locked;
    }

    private void insertAllocations(
            Long applicationId,
            String tenantId,
            Long orgId,
            Map<Long, BigDecimal> allocations,
            Long operatorId
    ) {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Long, BigDecimal> item : allocations.entrySet()) {
            PayableRow payable = repository.findPayable(item.getKey(), tenantId);
            repository.insertAllocation(new AllocationRow(
                    IdWorker.getId(), tenantId, orgId, applicationId,
                    item.getKey(), payable == null ? null : payable.number(),
                    item.getValue(), item.getValue(), BigDecimal.ZERO.setScale(2), ALLOCATION_RESERVED,
                    operatorId, now, 0
            ));
        }
    }

    private void insertEvidence(
            Long applicationId,
            String tenantId,
            Long orgId,
            List<EvidenceRequest> evidence,
            Long operatorId
    ) {
        if (evidence == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (EvidenceRequest item : evidence) {
            repository.insertEvidence(toEvidence(
                    IdWorker.getId(), tenantId, orgId, applicationId, item, operatorId, now));
        }
    }

    private EvidenceRow toEvidence(
            Long id,
            String tenantId,
            Long orgId,
            Long applicationId,
            EvidenceRequest request,
            Long operatorId,
            LocalDateTime now
    ) {
        return new EvidenceRow(
                id, tenantId, orgId, applicationId,
                normalizeEvidenceType(request.evidenceType()),
                normalizeText(request.sourceSystemCode()),
                normalizeText(request.sourceDocumentType()),
                normalizeText(request.sourceDocumentId()),
                normalizeText(request.sourceDocumentNo()),
                request.required() == null || request.required(),
                normalizeEvidenceStatus(request.verificationStatus()),
                normalizeText(request.remark()),
                operatorId, now, 0
        );
    }

    private void refreshEvidenceStatus(
            Long applicationId,
            String tenantId,
            Long operatorId
    ) {
        List<EvidenceRow> evidence = repository.findEvidence(applicationId, tenantId);
        List<EvidenceRow> required = evidence.stream().filter(EvidenceRow::required).toList();
        String status;
        if (required.isEmpty()) {
            status = EVIDENCE_PENDING;
        } else if (required.stream().anyMatch(item -> EVIDENCE_REJECTED.equals(item.verificationStatus()))) {
            status = "FAILED";
        } else if (required.stream().allMatch(item -> EVIDENCE_VERIFIED.equals(item.verificationStatus()))) {
            status = "PASSED";
        } else {
            status = EVIDENCE_PENDING;
        }
        repository.setEvidenceCheckStatus(applicationId, tenantId, status, operatorId);
    }

    private void validateControls(
            ApplicationRow app,
            List<AllocationRow> allocations
    ) {
        if (!"PASSED".equals(app.evidenceCheckStatus())) {
            throw new BizException("付款依据资料未完成核验");
        }
        if (!"PASSED".equals(app.budgetCheckStatus())
                && !"NOT_REQUIRED".equals(app.budgetCheckStatus())) {
            throw new BizException("资金预算校验未通过");
        }
        if ("PASSED".equals(app.budgetCheckStatus())
                && app.budgetAvailableAmount() != null
                && money(app.budgetAvailableAmount()).compareTo(money(app.amount())) < 0) {
            throw new BizException("资金预算可用额度不足");
        }
        validateReservations(allocations);
    }

    private void validateReservations(List<AllocationRow> allocations) {
        if (allocations.isEmpty()) {
            throw new BizException("付款申请缺少应付分配");
        }
        for (AllocationRow allocation : allocations) {
            if (!ALLOCATION_RESERVED.equals(allocation.status())
                    || money(allocation.reservedAmount()).compareTo(money(allocation.appliedAmount())) != 0) {
                throw new BizException("付款申请应付占用已失效，请重新提交");
            }
        }
    }

    private void releaseReservations(
            Long applicationId,
            String tenantId,
            Long operatorId
    ) {
        List<AllocationRow> allocations = repository.findAllocations(applicationId, tenantId);
        for (AllocationRow allocation : allocations.stream()
                .sorted(Comparator.comparing(AllocationRow::payableId)).toList()) {
            if (!ALLOCATION_RESERVED.equals(allocation.status())) {
                continue;
            }
            PayableRow payable = repository.lockPayable(allocation.payableId(), tenantId);
            if (payable == null) {
                throw new BizException("应付单不存在: " + allocation.payableId());
            }
            BigDecimal next = money(
                    nz(payable.reservedAmount()).subtract(nz(allocation.reservedAmount())));
            if (next.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("应付占用释放后小于0: " + payable.number());
            }
            repository.setPayableReserved(payable.id(), tenantId, next, operatorId);
            repository.updateAllocationReservation(
                    allocation.id(), tenantId, BigDecimal.ZERO,
                    ALLOCATION_RELEASED, operatorId);
        }
    }

    private void reReserve(
            Long applicationId,
            String tenantId,
            Long operatorId
    ) {
        List<AllocationRow> allocations = repository.findAllocations(applicationId, tenantId);
        for (AllocationRow allocation : allocations.stream()
                .sorted(Comparator.comparing(AllocationRow::payableId)).toList()) {
            if (ALLOCATION_RESERVED.equals(allocation.status())) {
                continue;
            }
            if (!ALLOCATION_RELEASED.equals(allocation.status())) {
                throw new BizException("付款申请分配状态不允许重新占用: " + allocation.status());
            }
            PayableRow payable = repository.lockPayable(allocation.payableId(), tenantId);
            validatePayable(payable, allocation.orgId());
            BigDecimal requested = money(allocation.appliedAmount());
            if (requested.compareTo(available(payable)) > 0) {
                throw new BizException("重新提交时应付可申请余额不足: " + payable.number());
            }
            repository.setPayableReserved(
                    payable.id(), tenantId,
                    money(nz(payable.reservedAmount()).add(requested)), operatorId);
            repository.updateAllocationReservation(
                    allocation.id(), tenantId, requested,
                    ALLOCATION_RESERVED, operatorId);
        }
    }

    private void validatePayable(PayableRow payable, Long orgId) {
        if (payable == null) {
            throw new BizException("应付单不存在");
        }
        if (!Objects.equals(orgId, payable.orgId())) {
            throw new BizException("付款申请组织与应付单不一致: " + payable.number());
        }
        if (!"FORMAL".equals(payable.type())) {
            throw new BizException("只有正式应付允许发起付款申请: " + payable.number());
        }
        if (!"OPEN".equals(payable.status()) && !"PARTIAL_SETTLED".equals(payable.status())) {
            throw new BizException("应付单状态不允许付款申请: " + payable.number());
        }
        if (!"AUDITED".equals(payable.approvalStatus())) {
            throw new BizException("只有已审核应付允许付款申请: " + payable.number());
        }
        if (!"VOUCHER_GENERATED".equals(payable.accountingStatus())
                && !"POSTED".equals(payable.accountingStatus())) {
            throw new BizException("应付核算尚未完成: " + payable.number());
        }
        if (money(payable.openAmount()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("应付未核销余额必须大于0: " + payable.number());
        }
        if (available(payable).compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("应付可申请余额异常: " + payable.number());
        }
    }

    private Map<Long, BigDecimal> normalizeAllocations(List<AllocationRequest> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            throw new BizException("付款申请至少需要一条应付分配");
        }
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (AllocationRequest item : allocations) {
            if (item == null || item.payableId() == null || item.amount() == null) {
                throw new BizException("应付分配字段不完整");
            }
            BigDecimal amount = money(item.amount());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("应付分配金额必须大于0");
            }
            result.merge(item.payableId(), amount, BigDecimal::add);
        }
        return result;
    }

    private ApplicationRow requireEditable(Long id, String tenantId) {
        ApplicationRow app = requireApplication(id, tenantId, true);
        if (!STATUS_DRAFT.equals(app.status()) && !STATUS_REJECTED.equals(app.status())) {
            throw new BizException("只有草稿或驳回状态付款申请可维护控制信息");
        }
        return app;
    }

    private ApplicationRow requireApplication(
            Long id,
            String tenantId,
            boolean forUpdate
    ) {
        ApplicationRow app = forUpdate
                ? repository.lockApplication(id, tenantId)
                : repository.findApplication(id, tenantId);
        if (app == null) {
            throw new BizException("付款申请不存在: " + id);
        }
        return app;
    }

    private Object approvedPayload(
            ApplicationRow app,
            List<AllocationRow> allocations
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentApplicationId", app.id());
        payload.put("paymentApplicationNo", app.number());
        payload.put("businessPartnerId", app.businessPartnerId());
        payload.put("businessPartnerCode", app.businessPartnerCode());
        payload.put("businessPartnerName", app.businessPartnerName());
        payload.put("currencyCode", app.currencyCode());
        payload.put("amount", app.amount());
        payload.put("plannedPayDate", app.plannedPayDate());
        payload.put("paymentMethod", app.paymentMethod());
        payload.put("fundPlanId", app.fundPlanId());
        payload.put("payeeBankAccountId", app.payeeBankAccountId());
        payload.put("budgetCheckId", app.budgetCheckId());
        payload.put("allocations", allocations.stream().map(item -> {
            Map<String, Object> allocation = new LinkedHashMap<>();
            allocation.put("paymentApplicationAllocationId", item.id());
            allocation.put("payableId", item.payableId());
            allocation.put("payableNumber", item.payableNumber());
            allocation.put("amount", item.appliedAmount());
            return allocation;
        }).toList());
        return payload;
    }

    private String normalizeBudgetStatus(String value) {
        String status = normalizeUpper(value);
        if (!List.of("PENDING", "PASSED", "FAILED", "NOT_REQUIRED").contains(status)) {
            throw new BizException("预算校验状态不支持: " + value);
        }
        return status;
    }

    private String normalizeEvidenceStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return EVIDENCE_PENDING;
        }
        String status = normalizeUpper(value);
        if (!List.of(EVIDENCE_PENDING, EVIDENCE_VERIFIED, EVIDENCE_REJECTED).contains(status)) {
            throw new BizException("依据材料核验状态不支持: " + value);
        }
        return status;
    }

    private String normalizeEvidenceType(String value) {
        String type = normalizeUpper(value);
        if (!List.of("CONTRACT", "INVOICE", "ACCEPTANCE", "INBOUND", "RECONCILIATION", "OTHER")
                .contains(type)) {
            throw new BizException("付款依据类型不支持: " + value);
        }
        return type;
    }

    private String normalizeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("状态/类型不能为空");
        }
        return value.trim().toUpperCase();
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal available(PayableRow payable) {
        return money(nz(payable.openAmount()).subtract(nz(payable.reservedAmount())));
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String number(Long id, LocalDate date) {
        String suffix = String.valueOf(id);
        suffix = suffix.length() <= 8 ? suffix : suffix.substring(suffix.length() - 8);
        return "PAYAPP-" + date.toString().replace("-", "") + "-" + suffix;
    }

    private String jsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException("预算校验快照序列化失败");
        }
    }
}
