package single.cjj.erp.procurement.delivery.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.procurement.delivery.entity.PurchaseDeliveryPlanEntity;
import single.cjj.erp.procurement.delivery.entity.PurchaseDeliveryPlanEntryEntity;
import single.cjj.erp.procurement.delivery.mapper.PurchaseDeliveryPlanEntryMapper;
import single.cjj.erp.procurement.delivery.mapper.PurchaseDeliveryPlanMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeliveryPlanFulfillmentService {

    private static final String PLAN_CONFIRMED = "CONFIRMED";
    private static final String PLAN_PARTIAL = "PARTIAL";
    private static final String PLAN_COMPLETE = "COMPLETE";

    private final PurchaseDeliveryPlanMapper planMapper;
    private final PurchaseDeliveryPlanEntryMapper entryMapper;

    public DeliveryPlanFulfillmentService(
            PurchaseDeliveryPlanMapper planMapper,
            PurchaseDeliveryPlanEntryMapper entryMapper
    ) {
        this.planMapper = planMapper;
        this.entryMapper = entryMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void validateReceiptReservation(
            String tenantId,
            Long purchaseOrderEntryId,
            BigDecimal requestedQuantity,
            BigDecimal existingReservedQuantity
    ) {
        PurchaseDeliveryPlanEntity plan =
                planMapper.selectActiveByOrderEntryForUpdate(
                        tenantId, purchaseOrderEntryId);
        if (plan == null) {
            return;
        }
        ensureReceivable(plan);

        List<PurchaseDeliveryPlanEntryEntity> entries =
                entryMapper.selectByOrderEntryForUpdate(
                        plan.getFid(), tenantId, purchaseOrderEntryId);
        BigDecimal remainingCommitment = entries.stream()
                .map(item -> nz(item.getFcommittedQuantity())
                        .subtract(nz(item.getFreceivedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal available = remainingCommitment
                .subtract(nz(existingReservedQuantity));
        if (requestedQuantity.compareTo(available) > 0) {
            throw new BizException("交付计划可收货承诺量不足，当前可预占: "
                    + plain(available));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(
            String tenantId,
            Long purchaseOrderEntryId,
            BigDecimal quantity,
            Long operatorId
    ) {
        PurchaseDeliveryPlanEntity plan =
                planMapper.selectActiveByOrderEntryForUpdate(
                        tenantId, purchaseOrderEntryId);
        if (plan == null) {
            return;
        }
        ensureReceivable(plan);

        List<PurchaseDeliveryPlanEntryEntity> entries =
                entryMapper.selectByOrderEntryForUpdate(
                        plan.getFid(), tenantId, purchaseOrderEntryId);
        BigDecimal totalRemaining = entries.stream()
                .map(item -> nz(item.getFcommittedQuantity())
                        .subtract(nz(item.getFreceivedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (quantity.compareTo(totalRemaining) > 0) {
            throw new BizException("确认收货数量超过供应商已确认交付承诺量");
        }

        BigDecimal remaining = quantity;
        for (PurchaseDeliveryPlanEntryEntity entry : entries) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal available = nz(entry.getFcommittedQuantity())
                    .subtract(nz(entry.getFreceivedQuantity()));
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal allocated = remaining.min(available);
            entry.setFreceivedQuantity(
                    nz(entry.getFreceivedQuantity()).add(allocated));
            entry.setFmodifyBy(operatorId);
            entry.setFmodifyTime(LocalDateTime.now());
            requireOne(entryMapper.updateById(entry), "交付计划收货反写");
            remaining = remaining.subtract(allocated);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("交付计划收货分配失败，剩余数量: "
                    + plain(remaining));
        }

        refreshPlanStatus(plan, tenantId, operatorId);
    }

    private void refreshPlanStatus(
            PurchaseDeliveryPlanEntity plan,
            String tenantId,
            Long operatorId
    ) {
        List<PurchaseDeliveryPlanEntryEntity> allEntries =
                entryMapper.selectByPlanIdForUpdate(
                        plan.getFid(), tenantId);
        boolean anyReceived = allEntries.stream().anyMatch(item ->
                nz(item.getFreceivedQuantity())
                        .compareTo(BigDecimal.ZERO) > 0);
        boolean complete = !allEntries.isEmpty()
                && allEntries.stream().allMatch(item ->
                nz(item.getFreceivedQuantity())
                        .compareTo(nz(item.getFcommittedQuantity())) >= 0);

        plan.setFstatus(complete
                ? PLAN_COMPLETE
                : anyReceived ? PLAN_PARTIAL : PLAN_CONFIRMED);
        plan.setFmodifyBy(operatorId);
        plan.setFmodifyTime(LocalDateTime.now());
        requireOne(planMapper.updateById(plan), "交付计划执行状态反写");
    }

    private void ensureReceivable(PurchaseDeliveryPlanEntity plan) {
        if (!(PLAN_CONFIRMED.equals(plan.getFstatus())
                || PLAN_PARTIAL.equals(plan.getFstatus()))) {
            throw new BizException("采购订单存在交付计划，但供应商承诺尚未确认，不能收货");
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String plain(BigDecimal value) {
        return nz(value).stripTrailingZeros().toPlainString();
    }

    private void requireOne(int affected, String action) {
        if (affected != 1) {
            throw new BizException(action + "失败，数据可能已被其他请求修改");
        }
    }
}
