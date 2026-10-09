package single.cjj.erp.procurement.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.procurement.delivery.service.DeliveryPlanFulfillmentService;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PurchaseOrderFulfillmentService {

    private static final String STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String APPROVAL_AUDITED = "AUDITED";

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderEntryMapper entryMapper;
    private final DeliveryPlanFulfillmentService deliveryPlanFulfillmentService;

    public PurchaseOrderFulfillmentService(
            PurchaseOrderMapper orderMapper,
            PurchaseOrderEntryMapper entryMapper,
            DeliveryPlanFulfillmentService deliveryPlanFulfillmentService
    ) {
        this.orderMapper = orderMapper;
        this.entryMapper = entryMapper;
        this.deliveryPlanFulfillmentService = deliveryPlanFulfillmentService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ReservedOrderLine reserveReceipt(
            String tenantId,
            Long orderEntryId,
            BigDecimal quantity,
            Long expectedPartnerId,
            Long expectedOrgId,
            Long operatorId
    ) {
        requirePositive(quantity, "收货预占数量");
        PurchaseOrderEntryEntity entry = requireEntryForUpdate(tenantId, orderEntryId);
        PurchaseOrderEntity order = requireOrderForUpdate(tenantId, entry.getFpurchaseOrderId());
        validateOrderForReceipt(order, expectedPartnerId, expectedOrgId);

        BigDecimal available = nz(entry.getFquantity())
                .subtract(nz(entry.getFreceivedQuantity()))
                .subtract(nz(entry.getFreceiptReservedQuantity()));
        if (quantity.compareTo(available) > 0) {
            throw new BizException("采购订单分录可收货数量不足，当前可用: " + plain(available));
        }
        deliveryPlanFulfillmentService.validateReceiptReservation(
                tenantId,
                orderEntryId,
                quantity,
                nz(entry.getFreceiptReservedQuantity())
        );

        entry.setFreceiptReservedQuantity(nz(entry.getFreceiptReservedQuantity()).add(quantity));
        touch(entry, operatorId);
        requireUpdated(entryMapper.updateById(entry), "采购订单分录收货预占");
        return new ReservedOrderLine(order, entry, available.subtract(quantity));
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseReceiptReservation(
            String tenantId,
            Long orderEntryId,
            BigDecimal quantity,
            Long operatorId
    ) {
        requirePositive(quantity, "释放收货预占数量");
        PurchaseOrderEntryEntity entry = requireEntryForUpdate(tenantId, orderEntryId);
        BigDecimal reserved = nz(entry.getFreceiptReservedQuantity());
        if (quantity.compareTo(reserved) > 0) {
            throw new BizException("释放收货预占数量超过当前预占量");
        }
        entry.setFreceiptReservedQuantity(reserved.subtract(quantity));
        touch(entry, operatorId);
        requireUpdated(entryMapper.updateById(entry), "释放采购订单收货预占");
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(
            String tenantId,
            Long orderEntryId,
            BigDecimal quantity,
            Long operatorId
    ) {
        requirePositive(quantity, "确认收货数量");
        PurchaseOrderEntryEntity entry = requireEntryForUpdate(tenantId, orderEntryId);
        PurchaseOrderEntity order = requireOrderForUpdate(tenantId, entry.getFpurchaseOrderId());
        validateOrderForReceipt(order, null, null);

        BigDecimal reserved = nz(entry.getFreceiptReservedQuantity());
        if (quantity.compareTo(reserved) > 0) {
            throw new BizException("确认收货数量超过预占数量，请重新生成/编辑收货单");
        }
        BigDecimal received = nz(entry.getFreceivedQuantity()).add(quantity);
        if (received.compareTo(nz(entry.getFquantity())) > 0) {
            throw new BizException("累计收货数量不能超过采购订单数量");
        }

        entry.setFreceiptReservedQuantity(reserved.subtract(quantity));
        entry.setFreceivedQuantity(received);
        touch(entry, operatorId);
        requireUpdated(entryMapper.updateById(entry), "采购订单收货反写");
        deliveryPlanFulfillmentService.confirmReceipt(
                tenantId, orderEntryId, quantity, operatorId);
        refreshReceiptStatus(order, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmAcceptedQuantity(
            String tenantId,
            Long orderEntryId,
            BigDecimal acceptedQuantity,
            Long operatorId
    ) {
        if (acceptedQuantity == null || acceptedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("验收合格/让步接收数量不能小于0");
        }
        if (acceptedQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        PurchaseOrderEntryEntity entry = requireEntryForUpdate(tenantId, orderEntryId);
        BigDecimal accepted = nz(entry.getFacceptedQuantity()).add(acceptedQuantity);
        if (accepted.compareTo(nz(entry.getFreceivedQuantity())) > 0) {
            throw new BizException("累计可入库验收数量不能超过累计收货数量");
        }
        entry.setFacceptedQuantity(accepted);
        touch(entry, operatorId);
        requireUpdated(entryMapper.updateById(entry), "采购订单验收反写");
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmInboundQuantity(
            String tenantId,
            Long orderEntryId,
            BigDecimal inboundQuantity,
            Long operatorId
    ) {
        requirePositive(inboundQuantity, "确认入库数量");
        PurchaseOrderEntryEntity entry = requireEntryForUpdate(tenantId, orderEntryId);
        BigDecimal inbound = nz(entry.getFinboundQuantity()).add(inboundQuantity);
        if (inbound.compareTo(nz(entry.getFacceptedQuantity())) > 0) {
            throw new BizException("累计入库数量不能超过已验收可入库数量");
        }
        entry.setFinboundQuantity(inbound);
        touch(entry, operatorId);
        requireUpdated(entryMapper.updateById(entry), "采购订单入库反写");
    }

    public PurchaseOrderEntryEntity requireEntry(String tenantId, Long orderEntryId) {
        PurchaseOrderEntryEntity entry = entryMapper.selectOne(new LambdaQueryWrapper<PurchaseOrderEntryEntity>()
                .eq(PurchaseOrderEntryEntity::getFid, orderEntryId)
                .eq(PurchaseOrderEntryEntity::getFtenantId, tenantId)
                .last("limit 1"));
        if (entry == null) {
            throw new BizException("采购订单分录不存在: " + orderEntryId);
        }
        return entry;
    }

    public PurchaseOrderEntity requireOrder(String tenantId, Long orderId) {
        PurchaseOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<PurchaseOrderEntity>()
                .eq(PurchaseOrderEntity::getFid, orderId)
                .eq(PurchaseOrderEntity::getFtenantId, tenantId)
                .last("limit 1"));
        if (order == null) {
            throw new BizException("采购订单不存在: " + orderId);
        }
        return order;
    }

    private PurchaseOrderEntryEntity requireEntryForUpdate(String tenantId, Long orderEntryId) {
        PurchaseOrderEntryEntity entry = entryMapper.selectByIdForUpdate(orderEntryId, tenantId);
        if (entry == null) {
            throw new BizException("采购订单分录不存在: " + orderEntryId);
        }
        return entry;
    }

    private PurchaseOrderEntity requireOrderForUpdate(String tenantId, Long orderId) {
        PurchaseOrderEntity order = orderMapper.selectByIdForUpdate(orderId, tenantId);
        if (order == null) {
            throw new BizException("采购订单不存在: " + orderId);
        }
        return order;
    }

    private void validateOrderForReceipt(
            PurchaseOrderEntity order,
            Long expectedPartnerId,
            Long expectedOrgId
    ) {
        if (!STATUS_EFFECTIVE.equals(order.getFstatus()) || !APPROVAL_AUDITED.equals(order.getFapprovalStatus())) {
            throw new BizException("仅已审核且已生效采购订单允许收货");
        }
        if (expectedPartnerId != null && !expectedPartnerId.equals(order.getFbusinessPartnerId())) {
            throw new BizException("收货供应商与采购订单供应商不一致");
        }
        if (expectedOrgId != null && !expectedOrgId.equals(order.getForgId())) {
            throw new BizException("收货组织与采购订单组织不一致");
        }
    }

    private void refreshReceiptStatus(PurchaseOrderEntity order, Long operatorId) {
        List<PurchaseOrderEntryEntity> entries = entryMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderEntryEntity>()
                        .eq(PurchaseOrderEntryEntity::getFpurchaseOrderId, order.getFid())
                        .orderByAsc(PurchaseOrderEntryEntity::getFlineNo)
        );
        boolean any = entries.stream().anyMatch(item -> nz(item.getFreceivedQuantity()).compareTo(BigDecimal.ZERO) > 0);
        boolean complete = !entries.isEmpty() && entries.stream().allMatch(item ->
                nz(item.getFreceivedQuantity()).compareTo(nz(item.getFquantity())) >= 0
        );
        order.setFreceiptStatus(complete ? "COMPLETE" : any ? "PARTIAL" : "NONE");
        order.setFmodifyBy(operatorId);
        order.setFmodifyTime(LocalDateTime.now());
        requireUpdated(orderMapper.updateById(order), "采购订单收货状态反写");
    }

    private void touch(PurchaseOrderEntryEntity entry, Long operatorId) {
        entry.setFmodifyBy(operatorId);
        entry.setFmodifyTime(LocalDateTime.now());
    }

    private void requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(field + "必须大于0");
        }
    }

    private void requireUpdated(int updated, String action) {
        if (updated != 1) {
            throw new BizException(action + "失败，数据可能已被其他请求修改");
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String plain(BigDecimal value) {
        return nz(value).stripTrailingZeros().toPlainString();
    }

    public record ReservedOrderLine(
            PurchaseOrderEntity order,
            PurchaseOrderEntryEntity entry,
            BigDecimal remainingAfterReservation
    ) {
    }
}