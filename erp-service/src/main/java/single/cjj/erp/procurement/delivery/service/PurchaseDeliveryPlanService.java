package single.cjj.erp.procurement.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.delivery.dto.PurchaseDeliveryPlanContracts.*;
import single.cjj.erp.procurement.delivery.entity.*;
import single.cjj.erp.procurement.delivery.mapper.*;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PurchaseDeliveryPlanService {

    private static final String ORDER_EFFECTIVE = "EFFECTIVE";
    private static final String ORDER_AUDITED = "AUDITED";
    private static final String ORDER_OPEN = "OPEN";

    private static final String PLAN_DRAFT = "DRAFT";
    private static final String PLAN_PUBLISHED = "PUBLISHED";
    private static final String PLAN_CHANGE_PROPOSED = "CHANGE_PROPOSED";
    private static final String PLAN_CONFIRMED = "CONFIRMED";
    private static final String PLAN_PARTIAL = "PARTIAL";
    private static final String PLAN_COMPLETE = "COMPLETE";
    private static final String PLAN_REJECTED = "REJECTED";
    private static final String PLAN_CANCELLED = "CANCELLED";

    private static final String RESPONSE_CONFIRM = "CONFIRM";
    private static final String RESPONSE_CHANGE = "CHANGE";
    private static final String RESPONSE_REJECT = "REJECT";

    private static final String RESPONSE_RECORDED = "RECORDED";
    private static final String RESPONSE_APPLIED = "APPLIED";
    private static final String RESPONSE_REJECTED = "REJECTED";

    private final PurchaseDeliveryPlanMapper planMapper;
    private final PurchaseDeliveryPlanEntryMapper planEntryMapper;
    private final SupplierDeliveryResponseMapper responseMapper;
    private final SupplierDeliveryResponseEntryMapper responseEntryMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderEntryMapper orderEntryMapper;
    private final BusinessEventOutboxService outboxService;

    public PurchaseDeliveryPlanService(
            PurchaseDeliveryPlanMapper planMapper,
            PurchaseDeliveryPlanEntryMapper planEntryMapper,
            SupplierDeliveryResponseMapper responseMapper,
            SupplierDeliveryResponseEntryMapper responseEntryMapper,
            PurchaseOrderMapper orderMapper,
            PurchaseOrderEntryMapper orderEntryMapper,
            BusinessEventOutboxService outboxService
    ) {
        this.planMapper = planMapper;
        this.planEntryMapper = planEntryMapper;
        this.responseMapper = responseMapper;
        this.responseEntryMapper = responseEntryMapper;
        this.orderMapper = orderMapper;
        this.orderEntryMapper = orderEntryMapper;
        this.outboxService = outboxService;
    }

    public Detail detail(Long fid, String tenantId) {
        PurchaseDeliveryPlanEntity plan = requirePlan(fid, tenantId, false);
        return new Detail(plan, listPlanEntries(fid));
    }

    public IPage<PurchaseDeliveryPlanEntity> page(
            String tenantId,
            Long orgId,
            Long purchaseOrderId,
            Long businessPartnerId,
            String status,
            int page,
            int size
    ) {
        String tenant = requireTenant(tenantId);
        return planMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.max(size, 1)),
                new LambdaQueryWrapper<PurchaseDeliveryPlanEntity>()
                        .eq(PurchaseDeliveryPlanEntity::getFtenantId, tenant)
                        .eq(orgId != null, PurchaseDeliveryPlanEntity::getForgId, orgId)
                        .eq(purchaseOrderId != null,
                                PurchaseDeliveryPlanEntity::getFpurchaseOrderId, purchaseOrderId)
                        .eq(businessPartnerId != null,
                                PurchaseDeliveryPlanEntity::getFbusinessPartnerId, businessPartnerId)
                        .eq(StringUtils.hasText(status),
                                PurchaseDeliveryPlanEntity::getFstatus, status)
                        .orderByDesc(PurchaseDeliveryPlanEntity::getFdate)
                        .orderByDesc(PurchaseDeliveryPlanEntity::getFid)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail create(CreateRequest request, Long operatorId) {
        String tenant = requireTenant(request.ftenantId());
        PurchaseOrderEntity order = requireOrderForPlanning(
                request.fpurchaseOrderId(), tenant
        );
        ensureNoActivePlan(tenant, order.getFid());

        LocalDate date = request.fdate() == null ? LocalDate.now() : request.fdate();
        Long planId = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : buildNumber("DP", date, planId);
        ensurePlanNumberUnique(tenant, number);

        List<PurchaseDeliveryPlanEntryEntity> entries = buildPlanEntries(
                planId, order, request.entries(), operatorId
        );

        PurchaseDeliveryPlanEntity plan = new PurchaseDeliveryPlanEntity();
        plan.setFid(planId);
        plan.setFtenantId(tenant);
        plan.setForgId(order.getForgId());
        plan.setFnumber(number);
        plan.setFdate(date);
        plan.setFpurchaseOrderId(order.getFid());
        plan.setFpurchaseOrderNo(order.getFnumber());
        plan.setFbusinessPartnerId(order.getFbusinessPartnerId());
        plan.setFbusinessPartnerCode(order.getFbusinessPartnerCode());
        plan.setFbusinessPartnerName(order.getFbusinessPartnerName());
        plan.setFcurrencyCode(order.getFcurrencyCode());
        plan.setFstatus(PLAN_DRAFT);
        plan.setFremark(trimToNull(request.fremark()));
        initAudit(plan, operatorId);
        requireOne(planMapper.insert(plan), "交付计划");
        insertPlanEntries(entries);
        return new Detail(plan, List.copyOf(entries));
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail update(Long fid, UpdateRequest request, Long operatorId) {
        String tenant = requireTenant(request.ftenantId());
        PurchaseDeliveryPlanEntity plan = requirePlan(fid, tenant, true);
        if (!(PLAN_DRAFT.equals(plan.getFstatus())
                || PLAN_REJECTED.equals(plan.getFstatus()))) {
            throw new BizException("仅草稿或供应商已拒绝的交付计划允许修改");
        }

        PurchaseOrderEntity order = requireOrderForPlanning(
                plan.getFpurchaseOrderId(), tenant
        );
        List<PurchaseDeliveryPlanEntryEntity> entries = buildPlanEntries(
                fid, order, request.entries(), operatorId
        );

        planEntryMapper.delete(
                new LambdaQueryWrapper<PurchaseDeliveryPlanEntryEntity>()
                        .eq(PurchaseDeliveryPlanEntryEntity::getFdeliveryPlanId, fid)
        );
        insertPlanEntries(entries);

        plan.setFstatus(PLAN_DRAFT);
        plan.setFcurrentResponseId(null);
        plan.setFpublishedTime(null);
        plan.setFconfirmedTime(null);
        plan.setFremark(trimToNull(request.fremark()));
        touch(plan, operatorId);
        requireOne(planMapper.updateById(plan), "交付计划");
        return new Detail(plan, List.copyOf(entries));
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail publish(Long fid, String tenantId, Long operatorId) {
        PurchaseDeliveryPlanEntity plan = requirePlan(fid, tenantId, true);
        if (!PLAN_DRAFT.equals(plan.getFstatus())) {
            throw new BizException("只有草稿交付计划允许发布");
        }
        requireOrderForPlanning(plan.getFpurchaseOrderId(), plan.getFtenantId());
        List<PurchaseDeliveryPlanEntryEntity> entries =
                planEntryMapper.selectByPlanIdForUpdate(
                        fid, plan.getFtenantId());
        if (entries.isEmpty()) {
            throw new BizException("交付计划至少需要一条分录");
        }

        plan.setFstatus(PLAN_PUBLISHED);
        plan.setFpublishedTime(LocalDateTime.now());
        plan.setFcurrentResponseId(null);
        touch(plan, operatorId);
        requireOne(planMapper.updateById(plan), "交付计划");

        outboxService.append(
                plan.getFtenantId(),
                plan.getForgId(),
                "PURCHASE_DELIVERY_PLAN_PUBLISHED",
                "PURCHASE_DELIVERY_PLAN",
                plan.getFid(),
                version(plan),
                "ERP_PURCHASE_DELIVERY_PLAN",
                plan.getFnumber(),
                plan.getFdate(),
                operatorId,
                planPayload(plan, entries)
        );
        return new Detail(plan, entries);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDetail recordSupplierResponse(
            Long planId,
            SupplierResponseRequest request,
            Long operatorId
    ) {
        String tenant = requireTenant(request.ftenantId());
        PurchaseDeliveryPlanEntity plan = requirePlan(planId, tenant, true);
        String type = request.fresponseType().trim().toUpperCase(Locale.ROOT);
        validateResponseTypeAndPlanState(plan, type);

        if (RESPONSE_CHANGE.equals(type)) {
            ensureNoReceiptReservation(plan);
        }

        LocalDate date = request.fdate() == null ? LocalDate.now() : request.fdate();
        Long responseId = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : buildNumber("DR", date, responseId);
        ensureResponseNumberUnique(tenant, number);

        List<PurchaseDeliveryPlanEntryEntity> planEntries =
                planEntryMapper.selectByPlanIdForUpdate(
                        planId, tenant);
        if (planEntries.isEmpty()) {
            throw new BizException("交付计划分录不存在");
        }

        SupplierDeliveryResponseEntity response =
                new SupplierDeliveryResponseEntity();
        response.setFid(responseId);
        response.setFtenantId(tenant);
        response.setForgId(plan.getForgId());
        response.setFdeliveryPlanId(planId);
        response.setFnumber(number);
        response.setFdate(date);
        response.setFresponseType(type);
        response.setFstatus(RESPONSE_CHANGE.equals(type)
                ? RESPONSE_RECORDED
                : RESPONSE_APPLIED);
        response.setFremark(trimToNull(request.fremark()));
        initAudit(response, operatorId);
        requireOne(responseMapper.insert(response), "供应商交付响应");

        List<SupplierDeliveryResponseEntryEntity> responseEntries =
                buildResponseEntries(
                        responseId,
                        plan,
                        planEntries,
                        type,
                        request.entries(),
                        operatorId
                );
        insertResponseEntries(responseEntries);

        plan.setFcurrentResponseId(responseId);
        if (RESPONSE_CONFIRM.equals(type)) {
            applyCommitment(
                    plan, planEntries, responseEntries,
                    "CONFIRMED", operatorId
            );
            response.setFstatus(RESPONSE_APPLIED);
            requireOne(responseMapper.updateById(response), "供应商交付响应");
        } else if (RESPONSE_CHANGE.equals(type)) {
            plan.setFstatus(PLAN_CHANGE_PROPOSED);
            touch(plan, operatorId);
            requireOne(planMapper.updateById(plan), "交付计划");
        } else {
            plan.setFstatus(PLAN_REJECTED);
            for (PurchaseDeliveryPlanEntryEntity entry : planEntries) {
                entry.setFresponseStatus("REJECTED");
                touch(entry, operatorId);
                requireOne(planEntryMapper.updateById(entry), "交付计划分录");
            }
            touch(plan, operatorId);
            requireOne(planMapper.updateById(plan), "交付计划");
        }

        outboxService.append(
                tenant,
                plan.getForgId(),
                "SUPPLIER_DELIVERY_RESPONSE_RECORDED",
                "SUPPLIER_DELIVERY_RESPONSE",
                responseId,
                version(response),
                "ERP_SUPPLIER_DELIVERY_RESPONSE",
                response.getFnumber(),
                response.getFdate(),
                operatorId,
                responsePayload(plan, response, responseEntries)
        );

        if (RESPONSE_CONFIRM.equals(type)) {
            appendPlanConfirmedEvent(plan, planEntries, operatorId);
        }
        return new ResponseDetail(response, responseEntries);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail acceptChangeResponse(
            Long planId,
            Long responseId,
            String tenantId,
            Long operatorId
    ) {
        String tenant = requireTenant(tenantId);
        PurchaseDeliveryPlanEntity plan = requirePlan(planId, tenant, true);
        if (!PLAN_CHANGE_PROPOSED.equals(plan.getFstatus())
                || !responseId.equals(plan.getFcurrentResponseId())) {
            throw new BizException("当前交付计划没有待接受的该供应商变更");
        }
        ensureNoReceiptReservation(plan);

        SupplierDeliveryResponseEntity response =
                responseMapper.selectByIdForUpdate(responseId, tenant);
        if (response == null
                || !planId.equals(response.getFdeliveryPlanId())
                || !RESPONSE_CHANGE.equals(response.getFresponseType())
                || !RESPONSE_RECORDED.equals(response.getFstatus())) {
            throw new BizException("供应商变更响应不存在或已处理");
        }

        List<PurchaseDeliveryPlanEntryEntity> planEntries =
                planEntryMapper.selectByPlanIdForUpdate(planId, tenant);
        List<SupplierDeliveryResponseEntryEntity> responseEntries =
                listResponseEntries(responseId);
        applyCommitment(
                plan, planEntries, responseEntries,
                "CHANGED", operatorId
        );

        response.setFstatus(RESPONSE_APPLIED);
        touch(response, operatorId);
        requireOne(responseMapper.updateById(response), "供应商交付响应");

        appendPlanConfirmedEvent(plan, planEntries, operatorId);
        return new Detail(plan, planEntries);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail rejectChangeResponse(
            Long planId,
            Long responseId,
            String tenantId,
            Long operatorId
    ) {
        String tenant = requireTenant(tenantId);
        PurchaseDeliveryPlanEntity plan = requirePlan(planId, tenant, true);
        if (!PLAN_CHANGE_PROPOSED.equals(plan.getFstatus())
                || !responseId.equals(plan.getFcurrentResponseId())) {
            throw new BizException("当前交付计划没有待拒绝的该供应商变更");
        }

        SupplierDeliveryResponseEntity response =
                responseMapper.selectByIdForUpdate(responseId, tenant);
        if (response == null
                || !RESPONSE_CHANGE.equals(response.getFresponseType())
                || !RESPONSE_RECORDED.equals(response.getFstatus())) {
            throw new BizException("供应商变更响应不存在或已处理");
        }

        response.setFstatus(RESPONSE_REJECTED);
        touch(response, operatorId);
        requireOne(responseMapper.updateById(response), "供应商交付响应");

        List<PurchaseDeliveryPlanEntryEntity> entries =
                planEntryMapper.selectByPlanIdForUpdate(planId, tenant);
        boolean allCommitted = !entries.isEmpty()
                && entries.stream().allMatch(item ->
                item.getFcommittedQuantity() != null
                        && item.getFcommittedDeliveryDate() != null);
        boolean anyReceived = entries.stream().anyMatch(item ->
                nz(item.getFreceivedQuantity()).compareTo(BigDecimal.ZERO) > 0);
        plan.setFstatus(allCommitted
                ? anyReceived ? PLAN_PARTIAL : PLAN_CONFIRMED
                : PLAN_PUBLISHED);
        plan.setFcurrentResponseId(null);
        touch(plan, operatorId);
        requireOne(planMapper.updateById(plan), "交付计划");
        return new Detail(plan, entries);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail cancel(Long fid, String tenantId, Long operatorId) {
        PurchaseDeliveryPlanEntity plan = requirePlan(fid, tenantId, true);
        if (PLAN_CANCELLED.equals(plan.getFstatus())) {
            return new Detail(plan, listPlanEntries(fid));
        }
        if (PLAN_COMPLETE.equals(plan.getFstatus())) {
            throw new BizException("已完成交付计划不能取消");
        }

        List<PurchaseDeliveryPlanEntryEntity> entries =
                planEntryMapper.selectByPlanIdForUpdate(
                        fid, plan.getFtenantId());
        boolean received = entries.stream().anyMatch(item ->
                nz(item.getFreceivedQuantity()).compareTo(BigDecimal.ZERO) > 0);
        if (received) {
            throw new BizException("已发生实际收货的交付计划不能直接取消");
        }
        ensureNoReceiptReservation(plan);

        plan.setFstatus(PLAN_CANCELLED);
        touch(plan, operatorId);
        requireOne(planMapper.updateById(plan), "交付计划");
        return new Detail(plan, entries);
    }

    public List<ResponseDetail> listResponses(Long planId, String tenantId) {
        PurchaseDeliveryPlanEntity plan = requirePlan(planId, tenantId, false);
        List<SupplierDeliveryResponseEntity> responses =
                responseMapper.selectList(
                        new LambdaQueryWrapper<SupplierDeliveryResponseEntity>()
                                .eq(SupplierDeliveryResponseEntity::getFtenantId,
                                        plan.getFtenantId())
                                .eq(SupplierDeliveryResponseEntity::getFdeliveryPlanId,
                                        planId)
                                .orderByAsc(SupplierDeliveryResponseEntity::getFdate)
                                .orderByAsc(SupplierDeliveryResponseEntity::getFid)
                );
        List<ResponseDetail> result = new ArrayList<>();
        for (SupplierDeliveryResponseEntity response : responses) {
            result.add(new ResponseDetail(
                    response, listResponseEntries(response.getFid())));
        }
        return List.copyOf(result);
    }

    private List<PurchaseDeliveryPlanEntryEntity> buildPlanEntries(
            Long planId,
            PurchaseOrderEntity order,
            List<EntryRequest> requests,
            Long operatorId
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException("交付计划至少需要一条分录");
        }

        List<PurchaseOrderEntryEntity> orderEntries =
                orderEntryMapper.selectList(
                        new LambdaQueryWrapper<PurchaseOrderEntryEntity>()
                                .eq(PurchaseOrderEntryEntity::getFpurchaseOrderId,
                                        order.getFid())
                                .orderByAsc(PurchaseOrderEntryEntity::getFlineNo)
                );
        if (orderEntries.isEmpty()) {
            throw new BizException("采购订单没有可计划分录");
        }

        Map<Long, PurchaseOrderEntryEntity> locked = new LinkedHashMap<>();
        for (PurchaseOrderEntryEntity source :
                orderEntries.stream()
                        .sorted(Comparator.comparing(PurchaseOrderEntryEntity::getFid))
                        .toList()) {
            PurchaseOrderEntryEntity current =
                    orderEntryMapper.selectByIdForUpdate(
                            source.getFid(), order.getFtenantId());
            if (current == null) {
                throw new BizException("采购订单分录不存在: " + source.getFid());
            }
            if (nz(current.getFreceivedQuantity()).compareTo(BigDecimal.ZERO) > 0
                    || nz(current.getFreceiptReservedQuantity())
                    .compareTo(BigDecimal.ZERO) > 0) {
                throw new BizException("已发生收货或收货预占的采购订单不能重新制定交付计划");
            }
            locked.put(current.getFid(), current);
        }

        Map<Long, BigDecimal> plannedByOrderEntry = new LinkedHashMap<>();
        List<EntryRequest> sorted = new ArrayList<>(requests);
        sorted.sort(Comparator
                .comparing(EntryRequest::fpurchaseOrderEntryId)
                .thenComparing(EntryRequest::fplannedDeliveryDate));

        List<PurchaseDeliveryPlanEntryEntity> result =
                new ArrayList<>(sorted.size());
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < sorted.size(); i++) {
            EntryRequest item = sorted.get(i);
            PurchaseOrderEntryEntity source =
                    locked.get(item.fpurchaseOrderEntryId());
            if (source == null) {
                throw new BizException("交付计划分录不属于当前采购订单: "
                        + item.fpurchaseOrderEntryId());
            }

            plannedByOrderEntry.merge(
                    source.getFid(),
                    item.fplannedQuantity(),
                    BigDecimal::add
            );

            PurchaseDeliveryPlanEntryEntity entry =
                    new PurchaseDeliveryPlanEntryEntity();
            entry.setFid(IdWorker.getId());
            entry.setFtenantId(order.getFtenantId());
            entry.setForgId(order.getForgId());
            entry.setFdeliveryPlanId(planId);
            entry.setFlineNo(i + 1);
            entry.setFpurchaseOrderId(order.getFid());
            entry.setFpurchaseOrderEntryId(source.getFid());
            entry.setFmaterialId(source.getFmaterialId());
            entry.setFmaterialCode(source.getFmaterialCode());
            entry.setFmaterialName(source.getFmaterialName());
            entry.setFspecification(source.getFspecification());
            entry.setFunitId(source.getFunitId());
            entry.setFplannedQuantity(item.fplannedQuantity());
            entry.setFplannedDeliveryDate(item.fplannedDeliveryDate());
            entry.setFresponseStatus("WAITING");
            entry.setFreceivedQuantity(BigDecimal.ZERO);
            entry.setFprojectId(source.getFprojectId());
            entry.setFcostCenterId(source.getFcostCenterId());
            entry.setFcreateBy(operatorId);
            entry.setFcreateTime(now);
            entry.setFmodifyBy(operatorId);
            entry.setFmodifyTime(now);
            entry.setFdeleteFlag(0);
            entry.setFversion(0);
            result.add(entry);
        }

        if (!plannedByOrderEntry.keySet().equals(locked.keySet())) {
            throw new BizException("交付计划必须覆盖采购订单全部分录");
        }
        for (Map.Entry<Long, PurchaseOrderEntryEntity> item : locked.entrySet()) {
            BigDecimal planned = nz(plannedByOrderEntry.get(item.getKey()));
            BigDecimal ordered = nz(item.getValue().getFquantity());
            if (planned.compareTo(ordered) != 0) {
                throw new BizException("交付计划数量必须完整覆盖采购订单分录数量: orderEntry="
                        + item.getKey() + ", orderQuantity=" + plain(ordered)
                        + ", plannedQuantity=" + plain(planned));
            }
        }
        return List.copyOf(result);
    }

    private List<SupplierDeliveryResponseEntryEntity> buildResponseEntries(
            Long responseId,
            PurchaseDeliveryPlanEntity plan,
            List<PurchaseDeliveryPlanEntryEntity> planEntries,
            String type,
            List<SupplierResponseEntryRequest> requests,
            Long operatorId
    ) {
        Map<Long, PurchaseDeliveryPlanEntryEntity> planById =
                new LinkedHashMap<>();
        for (PurchaseDeliveryPlanEntryEntity entry : planEntries) {
            planById.put(entry.getFid(), entry);
        }

        if (RESPONSE_REJECT.equals(type)) {
            if (requests == null || requests.isEmpty()) {
                return List.of();
            }
            return createResponseEntries(
                    responseId, plan, planById, type, requests, operatorId);
        }

        List<SupplierResponseEntryRequest> normalized;
        if (RESPONSE_CONFIRM.equals(type)
                && (requests == null || requests.isEmpty())) {
            normalized = planEntries.stream()
                    .map(item -> new SupplierResponseEntryRequest(
                            item.getFid(),
                            item.getFplannedQuantity(),
                            item.getFplannedDeliveryDate(),
                            null))
                    .toList();
        } else {
            normalized = requests == null ? List.of() : requests;
        }

        if (normalized.size() != planEntries.size()) {
            throw new BizException("供应商确认/变更必须覆盖交付计划全部分录");
        }

        List<SupplierDeliveryResponseEntryEntity> result =
                createResponseEntries(
                        responseId, plan, planById, type,
                        normalized, operatorId);

        if (RESPONSE_CONFIRM.equals(type)) {
            for (SupplierDeliveryResponseEntryEntity responseEntry : result) {
                PurchaseDeliveryPlanEntryEntity planEntry =
                        planById.get(responseEntry.getFdeliveryPlanEntryId());
                if (responseEntry.getFcommittedQuantity()
                        .compareTo(planEntry.getFplannedQuantity()) != 0
                        || !responseEntry.getFcommittedDeliveryDate()
                        .equals(planEntry.getFplannedDeliveryDate())) {
                    throw new BizException("CONFIRM 必须原样确认计划数量和日期；调整请使用 CHANGE");
                }
            }
        }

        if (RESPONSE_CHANGE.equals(type)) {
            validateChangedTotals(planEntries, result);
        }
        return List.copyOf(result);
    }

    private List<SupplierDeliveryResponseEntryEntity> createResponseEntries(
            Long responseId,
            PurchaseDeliveryPlanEntity plan,
            Map<Long, PurchaseDeliveryPlanEntryEntity> planById,
            String type,
            List<SupplierResponseEntryRequest> requests,
            Long operatorId
    ) {
        Set<Long> seen = new LinkedHashSet<>();
        List<SupplierDeliveryResponseEntryEntity> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < requests.size(); i++) {
            SupplierResponseEntryRequest item = requests.get(i);
            if (!seen.add(item.fdeliveryPlanEntryId())) {
                throw new BizException("供应商响应重复引用交付计划分录: "
                        + item.fdeliveryPlanEntryId());
            }
            PurchaseDeliveryPlanEntryEntity planEntry =
                    planById.get(item.fdeliveryPlanEntryId());
            if (planEntry == null) {
                throw new BizException("供应商响应分录不属于当前交付计划: "
                        + item.fdeliveryPlanEntryId());
            }

            BigDecimal quantity = item.fcommittedQuantity();
            LocalDate deliveryDate = item.fcommittedDeliveryDate();

            if (!RESPONSE_REJECT.equals(type)) {
                if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BizException("供应商承诺数量必须大于0");
                }
                if (deliveryDate == null) {
                    throw new BizException("供应商承诺交付日期不能为空");
                }
                if (quantity.compareTo(nz(planEntry.getFreceivedQuantity())) < 0) {
                    throw new BizException("供应商承诺数量不能小于该计划行已实际收货数量");
                }
            }

            SupplierDeliveryResponseEntryEntity responseEntry =
                    new SupplierDeliveryResponseEntryEntity();
            responseEntry.setFid(IdWorker.getId());
            responseEntry.setFtenantId(plan.getFtenantId());
            responseEntry.setForgId(plan.getForgId());
            responseEntry.setFresponseId(responseId);
            responseEntry.setFlineNo(i + 1);
            responseEntry.setFdeliveryPlanEntryId(planEntry.getFid());
            responseEntry.setFcommittedQuantity(quantity);
            responseEntry.setFcommittedDeliveryDate(deliveryDate);
            responseEntry.setFreason(trimToNull(item.freason()));
            responseEntry.setFcreateBy(operatorId);
            responseEntry.setFcreateTime(now);
            responseEntry.setFmodifyBy(operatorId);
            responseEntry.setFmodifyTime(now);
            responseEntry.setFdeleteFlag(0);
            responseEntry.setFversion(0);
            result.add(responseEntry);
        }
        return result;
    }

    private void validateChangedTotals(
            List<PurchaseDeliveryPlanEntryEntity> planEntries,
            List<SupplierDeliveryResponseEntryEntity> responseEntries
    ) {
        Map<Long, Long> planEntryToOrderEntry = new HashMap<>();
        Map<Long, BigDecimal> plannedTotals = new HashMap<>();
        for (PurchaseDeliveryPlanEntryEntity planEntry : planEntries) {
            planEntryToOrderEntry.put(
                    planEntry.getFid(), planEntry.getFpurchaseOrderEntryId());
            plannedTotals.merge(
                    planEntry.getFpurchaseOrderEntryId(),
                    planEntry.getFplannedQuantity(),
                    BigDecimal::add);
        }

        Map<Long, BigDecimal> committedTotals = new HashMap<>();
        for (SupplierDeliveryResponseEntryEntity responseEntry :
                responseEntries) {
            Long orderEntryId = planEntryToOrderEntry.get(
                    responseEntry.getFdeliveryPlanEntryId());
            committedTotals.merge(
                    orderEntryId,
                    responseEntry.getFcommittedQuantity(),
                    BigDecimal::add);
        }

        if (!committedTotals.keySet().equals(plannedTotals.keySet())) {
            throw new BizException("供应商变更必须覆盖全部采购订单分录");
        }
        for (Map.Entry<Long, BigDecimal> planned : plannedTotals.entrySet()) {
            BigDecimal committed = committedTotals.get(planned.getKey());
            if (committed.compareTo(planned.getValue()) != 0) {
                throw new BizException("供应商变更只能重排交付数量/日期，不能改变订单总承诺量: orderEntry="
                        + planned.getKey());
            }
        }
    }

    private void applyCommitment(
            PurchaseDeliveryPlanEntity plan,
            List<PurchaseDeliveryPlanEntryEntity> planEntries,
            List<SupplierDeliveryResponseEntryEntity> responseEntries,
            String responseStatus,
            Long operatorId
    ) {
        Map<Long, SupplierDeliveryResponseEntryEntity> responseByPlanEntry =
                new HashMap<>();
        for (SupplierDeliveryResponseEntryEntity responseEntry :
                responseEntries) {
            responseByPlanEntry.put(
                    responseEntry.getFdeliveryPlanEntryId(), responseEntry);
        }

        for (PurchaseDeliveryPlanEntryEntity planEntry : planEntries) {
            SupplierDeliveryResponseEntryEntity responseEntry =
                    responseByPlanEntry.get(planEntry.getFid());
            if (responseEntry == null) {
                throw new BizException("供应商响应缺少交付计划分录: "
                        + planEntry.getFid());
            }
            planEntry.setFcommittedQuantity(
                    responseEntry.getFcommittedQuantity());
            planEntry.setFcommittedDeliveryDate(
                    responseEntry.getFcommittedDeliveryDate());
            planEntry.setFresponseStatus(responseStatus);
            planEntry.setFlatestResponseEntryId(responseEntry.getFid());
            touch(planEntry, operatorId);
            requireOne(planEntryMapper.updateById(planEntry), "交付计划分录");
        }

        boolean anyReceived = planEntries.stream().anyMatch(item ->
                nz(item.getFreceivedQuantity()).compareTo(BigDecimal.ZERO) > 0);
        boolean complete = !planEntries.isEmpty()
                && planEntries.stream().allMatch(item ->
                nz(item.getFreceivedQuantity())
                        .compareTo(nz(item.getFcommittedQuantity())) >= 0);
        plan.setFstatus(complete
                ? PLAN_COMPLETE
                : anyReceived ? PLAN_PARTIAL : PLAN_CONFIRMED);
        plan.setFconfirmedTime(LocalDateTime.now());
        touch(plan, operatorId);
        requireOne(planMapper.updateById(plan), "交付计划");
    }

    private PurchaseOrderEntity requireOrderForPlanning(
            Long orderId,
            String tenantId
    ) {
        PurchaseOrderEntity order =
                orderMapper.selectByIdForUpdate(orderId, tenantId);
        if (order == null) {
            throw new BizException("采购订单不存在: " + orderId);
        }
        if (!ORDER_EFFECTIVE.equals(order.getFstatus())
                || !ORDER_AUDITED.equals(order.getFapprovalStatus())
                || !ORDER_OPEN.equals(order.getFcloseStatus())) {
            throw new BizException("只有已审核生效且未关闭采购订单允许制定交付计划");
        }
        return order;
    }

    private void ensureNoActivePlan(String tenantId, Long orderId) {
        Long count = planMapper.selectCount(
                new LambdaQueryWrapper<PurchaseDeliveryPlanEntity>()
                        .eq(PurchaseDeliveryPlanEntity::getFtenantId, tenantId)
                        .eq(PurchaseDeliveryPlanEntity::getFpurchaseOrderId, orderId)
                        .ne(PurchaseDeliveryPlanEntity::getFstatus, PLAN_CANCELLED)
        );
        if (count != null && count > 0) {
            throw new BizException("采购订单已存在未取消的交付计划");
        }
    }

    private void ensureNoReceiptReservation(
            PurchaseDeliveryPlanEntity plan
    ) {
        List<PurchaseOrderEntryEntity> orderEntries =
                orderEntryMapper.selectList(
                        new LambdaQueryWrapper<PurchaseOrderEntryEntity>()
                                .eq(PurchaseOrderEntryEntity::getFpurchaseOrderId,
                                        plan.getFpurchaseOrderId())
        );
        boolean reserved = orderEntries.stream().anyMatch(item ->
                nz(item.getFreceiptReservedQuantity())
                        .compareTo(BigDecimal.ZERO) > 0);
        if (reserved) {
            throw new BizException("已有收货单预占采购订单数量，当前不能变更/取消交付承诺");
        }
    }

    private void validateResponseTypeAndPlanState(
            PurchaseDeliveryPlanEntity plan,
            String type
    ) {
        if (PLAN_CHANGE_PROPOSED.equals(plan.getFstatus())) {
            throw new BizException("已有待处理供应商变更，请先接受或拒绝");
        }
        if (RESPONSE_CONFIRM.equals(type)) {
            if (!PLAN_PUBLISHED.equals(plan.getFstatus())) {
                throw new BizException("CONFIRM 仅适用于已发布且尚未确认的交付计划");
            }
            return;
        }
        if (RESPONSE_CHANGE.equals(type)) {
            if (!(PLAN_PUBLISHED.equals(plan.getFstatus())
                    || PLAN_CONFIRMED.equals(plan.getFstatus())
                    || PLAN_PARTIAL.equals(plan.getFstatus()))) {
                throw new BizException("CHANGE 仅适用于已发布/已确认/部分执行的交付计划");
            }
            return;
        }
        if (RESPONSE_REJECT.equals(type)) {
            if (!PLAN_PUBLISHED.equals(plan.getFstatus())) {
                throw new BizException("REJECT 仅适用于首次发布待确认的交付计划");
            }
            return;
        }
        throw new BizException("供应商响应类型仅支持 CONFIRM / CHANGE / REJECT");
    }

    private PurchaseDeliveryPlanEntity requirePlan(
            Long fid, String tenantId, boolean forUpdate
    ) {
        String tenant = requireTenant(tenantId);
        PurchaseDeliveryPlanEntity plan = forUpdate
                ? planMapper.selectByIdForUpdate(fid, tenant)
                : planMapper.selectOne(
                new LambdaQueryWrapper<PurchaseDeliveryPlanEntity>()
                        .eq(PurchaseDeliveryPlanEntity::getFid, fid)
                        .eq(PurchaseDeliveryPlanEntity::getFtenantId, tenant)
                        .last("limit 1"));
        if (plan == null) {
            throw new BizException("采购交付计划不存在: " + fid);
        }
        return plan;
    }

    private List<PurchaseDeliveryPlanEntryEntity> listPlanEntries(Long planId) {
        return planEntryMapper.selectList(
                new LambdaQueryWrapper<PurchaseDeliveryPlanEntryEntity>()
                        .eq(PurchaseDeliveryPlanEntryEntity::getFdeliveryPlanId,
                                planId)
                        .orderByAsc(PurchaseDeliveryPlanEntryEntity::getFlineNo)
        );
    }

    private List<SupplierDeliveryResponseEntryEntity> listResponseEntries(
            Long responseId
    ) {
        return responseEntryMapper.selectList(
                new LambdaQueryWrapper<SupplierDeliveryResponseEntryEntity>()
                        .eq(SupplierDeliveryResponseEntryEntity::getFresponseId,
                                responseId)
                        .orderByAsc(SupplierDeliveryResponseEntryEntity::getFlineNo)
        );
    }

    private void insertPlanEntries(
            List<PurchaseDeliveryPlanEntryEntity> entries
    ) {
        for (PurchaseDeliveryPlanEntryEntity entry : entries) {
            requireOne(planEntryMapper.insert(entry), "交付计划分录");
        }
    }

    private void insertResponseEntries(
            List<SupplierDeliveryResponseEntryEntity> entries
    ) {
        for (SupplierDeliveryResponseEntryEntity entry : entries) {
            requireOne(responseEntryMapper.insert(entry), "供应商响应分录");
        }
    }

    private void ensurePlanNumberUnique(String tenantId, String number) {
        Long count = planMapper.selectCount(
                new LambdaQueryWrapper<PurchaseDeliveryPlanEntity>()
                        .eq(PurchaseDeliveryPlanEntity::getFtenantId, tenantId)
                        .eq(PurchaseDeliveryPlanEntity::getFnumber, number)
        );
        if (count != null && count > 0) {
            throw new BizException("交付计划号已存在: " + number);
        }
    }

    private void ensureResponseNumberUnique(String tenantId, String number) {
        Long count = responseMapper.selectCount(
                new LambdaQueryWrapper<SupplierDeliveryResponseEntity>()
                        .eq(SupplierDeliveryResponseEntity::getFtenantId, tenantId)
                        .eq(SupplierDeliveryResponseEntity::getFnumber, number)
        );
        if (count != null && count > 0) {
            throw new BizException("供应商响应号已存在: " + number);
        }
    }

    private void appendPlanConfirmedEvent(
            PurchaseDeliveryPlanEntity plan,
            List<PurchaseDeliveryPlanEntryEntity> entries,
            Long operatorId
    ) {
        outboxService.append(
                plan.getFtenantId(),
                plan.getForgId(),
                "PURCHASE_DELIVERY_PLAN_CONFIRMED",
                "PURCHASE_DELIVERY_PLAN",
                plan.getFid(),
                version(plan),
                "ERP_PURCHASE_DELIVERY_PLAN",
                plan.getFnumber(),
                plan.getFdate(),
                operatorId,
                planPayload(plan, entries)
        );
    }

    private Map<String, Object> planPayload(
            PurchaseDeliveryPlanEntity plan,
            List<PurchaseDeliveryPlanEntryEntity> entries
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deliveryPlanId", plan.getFid());
        payload.put("deliveryPlanNo", plan.getFnumber());
        payload.put("purchaseOrderId", plan.getFpurchaseOrderId());
        payload.put("purchaseOrderNo", plan.getFpurchaseOrderNo());
        payload.put("businessPartnerId", plan.getFbusinessPartnerId());
        payload.put("businessPartnerCode", plan.getFbusinessPartnerCode());
        payload.put("businessPartnerName", plan.getFbusinessPartnerName());
        payload.put("status", plan.getFstatus());
        payload.put("entries", entries.stream().map(entry -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("deliveryPlanEntryId", entry.getFid());
            line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
            line.put("materialId", entry.getFmaterialId());
            line.put("materialCode", entry.getFmaterialCode());
            line.put("plannedQuantity", entry.getFplannedQuantity());
            line.put("plannedDeliveryDate", entry.getFplannedDeliveryDate());
            line.put("committedQuantity", entry.getFcommittedQuantity());
            line.put("committedDeliveryDate", entry.getFcommittedDeliveryDate());
            line.put("receivedQuantity", entry.getFreceivedQuantity());
            return line;
        }).toList());
        return payload;
    }

    private Map<String, Object> responsePayload(
            PurchaseDeliveryPlanEntity plan,
            SupplierDeliveryResponseEntity response,
            List<SupplierDeliveryResponseEntryEntity> entries
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deliveryPlanId", plan.getFid());
        payload.put("deliveryPlanNo", plan.getFnumber());
        payload.put("purchaseOrderId", plan.getFpurchaseOrderId());
        payload.put("responseId", response.getFid());
        payload.put("responseNo", response.getFnumber());
        payload.put("responseType", response.getFresponseType());
        payload.put("status", response.getFstatus());
        payload.put("entries", entries.stream().map(entry -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("responseEntryId", entry.getFid());
            line.put("deliveryPlanEntryId",
                    entry.getFdeliveryPlanEntryId());
            line.put("committedQuantity",
                    entry.getFcommittedQuantity());
            line.put("committedDeliveryDate",
                    entry.getFcommittedDeliveryDate());
            line.put("reason", entry.getFreason());
            return line;
        }).toList());
        return payload;
    }

    private String buildNumber(String prefix, LocalDate date, Long id) {
        String suffix = String.valueOf(id);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return prefix + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + suffix;
    }

    private String requireTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException("tenantId 不能为空");
        }
        return tenantId.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String plain(BigDecimal value) {
        return nz(value).stripTrailingZeros().toPlainString();
    }

    private long version(PurchaseDeliveryPlanEntity entity) {
        return entity.getFversion() == null
                ? 0L : entity.getFversion().longValue();
    }

    private long version(SupplierDeliveryResponseEntity entity) {
        return entity.getFversion() == null
                ? 0L : entity.getFversion().longValue();
    }

    private void initAudit(Object entity, Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        if (entity instanceof PurchaseDeliveryPlanEntity value) {
            value.setFcreateBy(operatorId);
            value.setFcreateTime(now);
            value.setFmodifyBy(operatorId);
            value.setFmodifyTime(now);
            value.setFdeleteFlag(0);
            value.setFversion(0);
        } else if (entity instanceof SupplierDeliveryResponseEntity value) {
            value.setFcreateBy(operatorId);
            value.setFcreateTime(now);
            value.setFmodifyBy(operatorId);
            value.setFmodifyTime(now);
            value.setFdeleteFlag(0);
            value.setFversion(0);
        }
    }

    private void touch(Object entity, Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        if (entity instanceof PurchaseDeliveryPlanEntity value) {
            value.setFmodifyBy(operatorId);
            value.setFmodifyTime(now);
        } else if (entity instanceof PurchaseDeliveryPlanEntryEntity value) {
            value.setFmodifyBy(operatorId);
            value.setFmodifyTime(now);
        } else if (entity instanceof SupplierDeliveryResponseEntity value) {
            value.setFmodifyBy(operatorId);
            value.setFmodifyTime(now);
        }
    }

    private void requireOne(int affected, String objectName) {
        if (affected != 1) {
            throw new BizException(objectName + "已被其他请求修改，请刷新后重试");
        }
    }
}
