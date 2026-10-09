package single.cjj.fi.fund.payment;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.fund.payment.PaymentOrderContracts.ActionRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.AllocationRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.AllocationView;
import single.cjj.fi.fund.payment.PaymentOrderContracts.BotpCreateRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.BotpDocument;
import single.cjj.fi.fund.payment.PaymentOrderContracts.CreateRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.Detail;
import single.cjj.fi.fund.payment.PaymentOrderContracts.LiquidityCheckRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.SubmitToBankRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.ChannelFailureRequest;
import single.cjj.fi.fund.payment.PaymentOrderRepository.PaymentApplicationRow;
import single.cjj.fi.fund.payment.PaymentOrderRepository.PaymentOrderAllocationRow;
import single.cjj.fi.fund.payment.PaymentOrderRepository.PaymentOrderRow;

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
import java.util.UUID;

@Service
public class PaymentOrderService {

    private static final String ORDERED = "ORDERED";
    private static final String RELEASED = "RELEASED";

    private final PaymentOrderRepository repository;
    private final ObjectMapper objectMapper;

    public PaymentOrderService(
            PaymentOrderRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail create(CreateRequest request, Long operatorId) {
        Long id = IdWorker.getId();
        Map<Long, BigDecimal> allocations = normalizeAllocations(request.allocations());
        List<PaymentApplicationRow> applications = reserveApplications(
                request.tenantId(), request.orgId(), allocations, operatorId);
        PaymentApplicationRow first = applications.get(0);
        BigDecimal total = allocations.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        LocalDate date = request.orderDate() == null ? LocalDate.now() : request.orderDate();
        LocalDateTime now = LocalDateTime.now();

        PaymentOrderRow order = new PaymentOrderRow(
                id, request.tenantId(), request.orgId(), number(id, date), date,
                first.businessPartnerId(), first.businessPartnerCode(), first.businessPartnerName(),
                first.currencyCode(), total, normalizeRequired(request.paymentMethod(), "付款方式"),
                normalizeText(request.payerBankAccountId()),
                first.payeeBankAccountId(),
                normalizeText(request.payeeAccountName()) == null
                        ? first.payeeAccountName()
                        : normalizeText(request.payeeAccountName()),
                normalizeText(request.payeeBankName()) == null
                        ? first.payeeBankName()
                        : normalizeText(request.payeeBankName()),
                normalizeText(request.payeeBankAccountNo()) == null
                        ? first.payeeBankAccountNo()
                        : normalizeText(request.payeeBankAccountNo()),
                normalizeText(request.fundPlanId()) == null
                        ? first.fundPlanId()
                        : normalizeText(request.fundPlanId()),
                request.plannedPayDate() == null
                        ? first.plannedPayDate()
                        : request.plannedPayDate(),
                "PENDING", null, null, null,
                "DRAFT", "DRAFT",
                null, null, "NOT_SENT", null,
                null, null, null, null,
                "UNMATCHED", null, null,
                null, null, null, null, null,
                normalizeText(request.remark()), null,
                operatorId, now, 0
        );
        repository.insertOrder(order);
        insertAllocations(id, request.tenantId(), request.orgId(), allocations, operatorId);
        return detail(id, request.tenantId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail createFromBotp(BotpCreateRequest request) {
        PaymentOrderRow existing = repository.findByIdempotency(
                request.tenantId(), request.idempotencyKey());
        if (existing != null) {
            return detail(existing.id(), request.tenantId());
        }

        PaymentApplicationRow source = repository.lockApplication(
                request.paymentApplicationId(), request.tenantId());
        validateApplication(source, request.orgId());
        existing = repository.findByIdempotency(request.tenantId(), request.idempotencyKey());
        if (existing != null) {
            return detail(existing.id(), request.tenantId());
        }

        BigDecimal amount = money(request.amount());
        BigDecimal available = availableOrder(source);
        if (amount.compareTo(available) > 0) {
            throw new BizException("付款单金额超过付款申请可下推余额，当前可用: "
                    + available.stripTrailingZeros().toPlainString());
        }

        BigDecimal ordered = money(nz(source.orderedAmount()).add(amount));
        repository.updateApplicationOrdered(
                source.id(), source.tenantId(), ordered,
                executionStatus(source.amount(), ordered), request.operatorId());

        Long id = IdWorker.getId();
        LocalDate date = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        PaymentOrderRow order = new PaymentOrderRow(
                id, request.tenantId(), request.orgId(), number(id, date), date,
                source.businessPartnerId(), source.businessPartnerCode(), source.businessPartnerName(),
                source.currencyCode(), amount,
                normalizeText(request.paymentMethod()) == null
                        ? normalizeRequired(source.paymentMethod(), "付款方式")
                        : normalizeRequired(request.paymentMethod(), "付款方式"),
                normalizeText(request.payerBankAccountId()),
                source.payeeBankAccountId(), source.payeeAccountName(),
                source.payeeBankName(), source.payeeBankAccountNo(),
                source.fundPlanId(),
                request.plannedPayDate() == null ? source.plannedPayDate() : request.plannedPayDate(),
                "PENDING", null, null, null,
                "DRAFT", "DRAFT",
                null, null, "NOT_SENT", null,
                null, null, null, null,
                "UNMATCHED", null, null,
                request.idempotencyKey(), request.sourceSystem(), request.sourceDocumentType(),
                request.sourceDocumentId(), request.sourceExecutionId(),
                "BOTP来源: " + source.number(), null,
                request.operatorId(), now, 0
        );
        repository.insertOrder(order);
        repository.insertAllocation(new PaymentOrderAllocationRow(
                IdWorker.getId(), request.tenantId(), request.orgId(), id,
                source.id(), source.number(), amount, ORDERED,
                request.operatorId(), now, 0
        ));
        return detail(id, request.tenantId());
    }

    public Detail detail(Long fid, String tenantId) {
        PaymentOrderRow row = requireOrder(fid, tenantId, false);
        List<AllocationView> allocations = repository.findAllocations(fid, tenantId).stream()
                .map(item -> new AllocationView(
                        item.id(), item.paymentApplicationId(),
                        item.paymentApplicationNumber(), item.amount(), item.status()))
                .toList();
        return toDetail(row, allocations);
    }

    public List<Detail> list(String tenantId, Long orgId, String status, int limit) {
        return repository.listOrders(tenantId, orgId, status, limit).stream()
                .map(row -> toDetail(
                        row,
                        repository.findAllocations(row.id(), tenantId).stream()
                                .map(item -> new AllocationView(
                                        item.id(), item.paymentApplicationId(),
                                        item.paymentApplicationNumber(),
                                        item.amount(), item.status()))
                                .toList()
                ))
                .toList();
    }

    public Detail findByIdempotency(String tenantId, String key) {
        PaymentOrderRow row = repository.findByIdempotency(tenantId, key);
        return row == null ? null : detail(row.id(), tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail recordLiquidityCheck(
            Long orderId,
            String tenantId,
            LiquidityCheckRequest request,
            Long operatorId
    ) {
        PaymentOrderRow order = requireOrder(orderId, tenantId, true);
        if (!List.of("DRAFT", "SUBMITTED").contains(order.status())) {
            throw new BizException("只有草稿或已提交付款单可维护资金头寸校验");
        }
        String status = normalizeControlStatus(request.status());
        if ("PASSED".equals(status)) {
            if (request.availableAmount() == null) {
                throw new BizException("资金头寸校验通过时必须返回可用金额");
            }
            if (money(request.availableAmount()).compareTo(money(order.amount())) < 0) {
                throw new BizException("可用资金头寸小于付款单金额");
            }
        }
        repository.updateLiquidity(
                orderId, tenantId, status, normalizeText(request.checkId()),
                request.availableAmount(), normalizeText(request.message()),
                jsonOrNull(request.snapshot()), operatorId);
        return detail(orderId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail submit(Long orderId, String tenantId, ActionRequest request) {
        Long operatorId = request == null ? null : request.operatorId();
        PaymentOrderRow order = requireOrder(orderId, tenantId, true);
        if (!"DRAFT".equals(order.status()) && !"REJECTED".equals(order.status())) {
            throw new BizException("只有草稿或驳回付款单可提交");
        }
        if ("REJECTED".equals(order.status())) {
            reReserveApplications(orderId, tenantId, operatorId);
            order = requireOrder(orderId, tenantId, true);
        }
        validatePaymentExecutionFields(order);
        repository.updateOrderState(
                orderId, tenantId, "SUBMITTED", "SUBMITTED",
                null, null, null, LocalDateTime.now(), operatorId);
        return detail(orderId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail audit(Long orderId, String tenantId, ActionRequest request) {
        Long operatorId = request == null ? null : request.operatorId();
        PaymentOrderRow order = requireOrder(orderId, tenantId, true);
        if (!"SUBMITTED".equals(order.status())
                || !"SUBMITTED".equals(order.approvalStatus())) {
            throw new BizException("只有已提交付款单可审核");
        }
        validatePaymentExecutionFields(order);
        if (!"PASSED".equals(order.liquidityCheckStatus())
                && !"NOT_REQUIRED".equals(order.liquidityCheckStatus())) {
            throw new BizException("资金头寸校验未通过");
        }
        if ("PASSED".equals(order.liquidityCheckStatus())
                && order.liquidityAvailableAmount() != null
                && money(order.liquidityAvailableAmount()).compareTo(money(order.amount())) < 0) {
            throw new BizException("资金头寸可用金额不足");
        }
        repository.updateOrderState(
                orderId, tenantId, "AUDITED", "AUDITED",
                null, operatorId, LocalDateTime.now(), null, operatorId);
        return detail(orderId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail reject(Long orderId, String tenantId, ActionRequest request) {
        Long operatorId = request == null ? null : request.operatorId();
        PaymentOrderRow order = requireOrder(orderId, tenantId, true);
        if (!"SUBMITTED".equals(order.status())) {
            throw new BizException("只有已提交付款单可驳回");
        }
        releaseApplications(orderId, tenantId, operatorId);
        repository.updateOrderState(
                orderId, tenantId, "REJECTED", "REJECTED",
                normalizeText(request == null ? null : request.reason()),
                null, null, null, operatorId);
        return detail(orderId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail cancel(Long orderId, String tenantId, ActionRequest request) {
        Long operatorId = request == null ? null : request.operatorId();
        PaymentOrderRow order = requireOrder(orderId, tenantId, true);
        if (!"DRAFT".equals(order.status()) && !"REJECTED".equals(order.status())) {
            throw new BizException("只有草稿或驳回付款单可取消");
        }
        releaseApplications(orderId, tenantId, operatorId);
        repository.updateOrderState(
                orderId, tenantId, "CANCELLED", order.approvalStatus(),
                normalizeText(request == null ? null : request.reason()),
                null, null, null, operatorId);
        return detail(orderId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail submitToBank(
            Long orderId,
            String tenantId,
            SubmitToBankRequest request
    ) {
        PaymentOrderRow order = requireOrder(orderId, tenantId, true);
        if (!"AUDITED".equals(order.status())
                || !"AUDITED".equals(order.approvalStatus())) {
            throw new BizException("只有已审核付款单可以提交支付渠道");
        }
        validatePaymentExecutionFields(order);
        String channelRequestId = normalizeText(request.channelRequestId());
        if (channelRequestId == null) {
            channelRequestId = "PAYCH-" + orderId + "-"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
        repository.markPaying(
                orderId, tenantId, normalizeRequired(request.channelCode(), "支付渠道"),
                channelRequestId, request.operatorId());
        return detail(orderId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail markChannelFailed(
            Long orderId,
            String tenantId,
            ChannelFailureRequest request
    ) {
        PaymentOrderRow order = requireOrder(orderId, tenantId, true);
        if (!"PAYING".equals(order.status())) {
            throw new BizException("只有支付中付款单可记录渠道失败");
        }
        repository.markChannelFailed(
                orderId, tenantId, normalizeText(request.errorMessage()), request.operatorId());
        return detail(orderId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public BotpDocument recomputeApplicationOrdered(
            Long applicationId,
            String tenantId,
            Long operatorId
    ) {
        PaymentApplicationRow application = repository.lockApplication(applicationId, tenantId);
        if (application == null) {
            throw new BizException("付款申请不存在: " + applicationId);
        }
        BigDecimal ordered = money(repository.sumOrderedByApplication(applicationId, tenantId));
        if (ordered.compareTo(money(application.amount())) > 0) {
            throw new BizException("付款单占用超过付款申请金额: " + application.number());
        }
        repository.updateApplicationOrdered(
                applicationId, tenantId, ordered,
                executionStatus(application.amount(), ordered), operatorId);
        return botpApplication(applicationId);
    }

    public BotpDocument botpApplication(Long applicationId) {
        PaymentApplicationRow application = repository.findApplicationById(applicationId);
        if (application == null) {
            throw new BizException("付款申请不存在: " + applicationId);
        }
        return new BotpDocument(
                "PA:" + application.id(), application.id(), application.number(),
                application.date(), application.tenantId(), application.orgId(),
                application.businessPartnerId(), application.businessPartnerCode(),
                application.businessPartnerName(), application.currencyCode(),
                application.amount(), nz(application.orderedAmount()), availableOrder(application),
                application.status(), application.approvalStatus(), application.executionStatus(),
                application.paymentMethod(), application.plannedPayDate(),
                null, application.payeeBankAccountId(),
                application.payeeAccountName(), application.payeeBankName(),
                application.payeeBankAccountNo(),
                null, null, null, null, application.version()
        );
    }

    public BotpDocument botpOrder(Long orderId) {
        PaymentOrderRow order = repository.findOrderById(orderId);
        if (order == null) {
            throw new BizException("付款单不存在: " + orderId);
        }
        return new BotpDocument(
                "PAYORD:" + order.id(), order.id(), order.number(), order.date(),
                order.tenantId(), order.orgId(),
                order.businessPartnerId(), order.businessPartnerCode(),
                order.businessPartnerName(), order.currencyCode(),
                order.amount(), null, null,
                order.status(), order.approvalStatus(), null,
                order.paymentMethod(), order.plannedPayDate(),
                order.payerBankAccountId(), order.payeeBankAccountId(),
                order.payeeAccountName(), order.payeeBankName(), order.payeeBankAccountNo(),
                order.sourceDocumentType(), order.sourceDocumentId(),
                order.sourceExecutionId(), order.botpIdempotencyKey(), order.version()
        );
    }

    private List<PaymentApplicationRow> reserveApplications(
            String tenantId,
            Long orgId,
            Map<Long, BigDecimal> allocations,
            Long operatorId
    ) {
        List<PaymentApplicationRow> locked = new ArrayList<>();
        PaymentApplicationRow reference = null;
        for (Long applicationId : allocations.keySet().stream().sorted().toList()) {
            PaymentApplicationRow application = repository.lockApplication(applicationId, tenantId);
            validateApplication(application, orgId);
            if (reference == null) {
                reference = application;
            } else {
                if (!Objects.equals(reference.businessPartnerId(), application.businessPartnerId())) {
                    throw new BizException("一张付款单只能包含同一供应商的付款申请");
                }
                if (!Objects.equals(reference.currencyCode(), application.currencyCode())) {
                    throw new BizException("一张付款单只能包含同一币种的付款申请");
                }
                if (!Objects.equals(reference.paymentMethod(), application.paymentMethod())) {
                    throw new BizException("一张付款单只能包含相同付款方式的付款申请");
                }
                if (!Objects.equals(reference.payeeBankAccountId(), application.payeeBankAccountId())) {
                    throw new BizException("一张付款单只能包含相同收款账户的付款申请");
                }
            }
            BigDecimal requested = allocations.get(applicationId);
            BigDecimal available = availableOrder(application);
            if (requested.compareTo(available) > 0) {
                throw new BizException("付款申请 " + application.number()
                        + " 可下推金额不足，当前可用: "
                        + available.stripTrailingZeros().toPlainString());
            }
            locked.add(application);
        }
        for (PaymentApplicationRow application : locked) {
            BigDecimal next = money(
                    nz(application.orderedAmount()).add(allocations.get(application.id())));
            repository.updateApplicationOrdered(
                    application.id(), tenantId, next,
                    executionStatus(application.amount(), next), operatorId);
        }
        return locked;
    }

    private void insertAllocations(
            Long orderId,
            String tenantId,
            Long orgId,
            Map<Long, BigDecimal> allocations,
            Long operatorId
    ) {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Long, BigDecimal> item : allocations.entrySet()) {
            PaymentApplicationRow application = repository.findApplication(item.getKey(), tenantId);
            repository.insertAllocation(new PaymentOrderAllocationRow(
                    IdWorker.getId(), tenantId, orgId, orderId, item.getKey(),
                    application == null ? null : application.number(),
                    item.getValue(), ORDERED, operatorId, now, 0
            ));
        }
    }

    private void releaseApplications(
            Long orderId,
            String tenantId,
            Long operatorId
    ) {
        List<PaymentOrderAllocationRow> allocations = repository.findAllocations(orderId, tenantId);
        for (PaymentOrderAllocationRow allocation : allocations.stream()
                .sorted(Comparator.comparing(PaymentOrderAllocationRow::paymentApplicationId))
                .toList()) {
            if (!ORDERED.equals(allocation.status())) {
                continue;
            }
            PaymentApplicationRow application = repository.lockApplication(
                    allocation.paymentApplicationId(), tenantId);
            if (application == null) {
                throw new BizException("付款申请不存在: " + allocation.paymentApplicationId());
            }
            BigDecimal next = money(
                    nz(application.orderedAmount()).subtract(nz(allocation.amount())));
            if (next.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("付款申请已下推金额释放后小于0: " + application.number());
            }
            repository.updateApplicationOrdered(
                    application.id(), tenantId, next,
                    executionStatus(application.amount(), next), operatorId);
            repository.updateAllocationStatus(
                    allocation.id(), tenantId, RELEASED, operatorId);
        }
    }

    private void reReserveApplications(
            Long orderId,
            String tenantId,
            Long operatorId
    ) {
        List<PaymentOrderAllocationRow> allocations = repository.findAllocations(orderId, tenantId);
        for (PaymentOrderAllocationRow allocation : allocations.stream()
                .sorted(Comparator.comparing(PaymentOrderAllocationRow::paymentApplicationId))
                .toList()) {
            if (ORDERED.equals(allocation.status())) {
                continue;
            }
            if (!RELEASED.equals(allocation.status())) {
                throw new BizException("付款单分配状态不允许重新占用: " + allocation.status());
            }
            PaymentApplicationRow application = repository.lockApplication(
                    allocation.paymentApplicationId(), tenantId);
            validateApplication(application, allocation.orgId());
            BigDecimal requested = money(allocation.amount());
            if (requested.compareTo(availableOrder(application)) > 0) {
                throw new BizException("重新提交时付款申请可下推余额不足: " + application.number());
            }
            BigDecimal next = money(nz(application.orderedAmount()).add(requested));
            repository.updateApplicationOrdered(
                    application.id(), tenantId, next,
                    executionStatus(application.amount(), next), operatorId);
            repository.updateAllocationStatus(
                    allocation.id(), tenantId, ORDERED, operatorId);
        }
    }

    private void validateApplication(PaymentApplicationRow application, Long orgId) {
        if (application == null) {
            throw new BizException("付款申请不存在");
        }
        if (!Objects.equals(orgId, application.orgId())) {
            throw new BizException("付款单组织与付款申请不一致: " + application.number());
        }
        if (!"APPROVED".equals(application.status())
                || !"AUDITED".equals(application.approvalStatus())) {
            throw new BizException("只有已审批付款申请允许生成付款单: " + application.number());
        }
        if (availableOrder(application).compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("付款申请可下推余额异常: " + application.number());
        }
    }

    private void validatePaymentExecutionFields(PaymentOrderRow order) {
        normalizeRequired(order.paymentMethod(), "付款方式");
        normalizeRequired(order.payerBankAccountId(), "付款方银行账户");
        normalizeRequired(order.payeeBankAccountId(), "收款账户ID");
        normalizeRequired(order.payeeAccountName(), "收款账户名称");
        normalizeRequired(order.payeeBankAccountNo(), "收款账号");
    }

    private Map<Long, BigDecimal> normalizeAllocations(List<AllocationRequest> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            throw new BizException("付款单至少需要一条付款申请分配");
        }
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (AllocationRequest item : allocations) {
            if (item == null || item.paymentApplicationId() == null || item.amount() == null) {
                throw new BizException("付款申请分配字段不完整");
            }
            BigDecimal amount = money(item.amount());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("付款申请分配金额必须大于0");
            }
            result.merge(item.paymentApplicationId(), amount, BigDecimal::add);
        }
        return result;
    }

    private PaymentOrderRow requireOrder(Long id, String tenantId, boolean forUpdate) {
        PaymentOrderRow order = forUpdate
                ? repository.lockOrder(id, tenantId)
                : repository.findOrder(id, tenantId);
        if (order == null) {
            throw new BizException("付款单不存在: " + id);
        }
        return order;
    }

    private Detail toDetail(
            PaymentOrderRow row,
            List<AllocationView> allocations
    ) {
        return new Detail(
                row.id(), row.tenantId(), row.orgId(), row.number(), row.date(),
                row.businessPartnerId(), row.businessPartnerCode(), row.businessPartnerName(),
                row.currencyCode(), row.amount(), row.paymentMethod(),
                row.payerBankAccountId(), row.payeeBankAccountId(),
                row.payeeAccountName(), row.payeeBankName(), row.payeeBankAccountNo(),
                row.fundPlanId(), row.plannedPayDate(),
                row.liquidityCheckStatus(), row.liquidityCheckId(),
                row.liquidityAvailableAmount(), row.liquidityCheckMessage(),
                row.status(), row.approvalStatus(),
                row.channelCode(), row.channelRequestId(),
                row.channelStatus(), row.channelError(),
                row.submittedTime(), row.auditBy(), row.auditTime(), row.payingTime(),
                row.bankMatchStatus(), row.reconciliationBatchId(), row.reconciliationCaseId(),
                row.botpIdempotencyKey(), row.sourceSystemCode(),
                row.sourceDocumentType(), row.sourceDocumentId(), row.sourceExecutionId(),
                row.remark(), row.rejectReason(), row.version(), allocations
        );
    }

    private String executionStatus(BigDecimal amount, BigDecimal ordered) {
        BigDecimal total = money(amount);
        BigDecimal used = money(ordered);
        if (used.compareTo(BigDecimal.ZERO) <= 0) {
            return "NOT_EXECUTED";
        }
        if (used.compareTo(total) < 0) {
            return "PARTIAL";
        }
        if (used.compareTo(total) == 0) {
            return "COMPLETE";
        }
        throw new BizException("付款申请已下推金额超过申请金额");
    }

    private BigDecimal availableOrder(PaymentApplicationRow application) {
        return money(nz(application.amount()).subtract(nz(application.orderedAmount())));
    }

    private String normalizeControlStatus(String value) {
        String status = normalizeUpper(value, "资金头寸状态");
        if (!List.of("PENDING", "PASSED", "FAILED", "NOT_REQUIRED").contains(status)) {
            throw new BizException("资金头寸状态不支持: " + value);
        }
        return status;
    }

    private String normalizeUpper(String value, String field) {
        return normalizeRequired(value, field).toUpperCase();
    }

    private String normalizeRequired(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(field + "不能为空");
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
        return "PAYORD-" + date.toString().replace("-", "") + "-" + suffix;
    }

    private String jsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException("资金头寸快照序列化失败");
        }
    }
}
