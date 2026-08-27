package single.cjj.erp.procurement.delivery.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseDeliveryPlanServiceTest {

    @Mock PurchaseDeliveryPlanMapper planMapper;
    @Mock PurchaseDeliveryPlanEntryMapper planEntryMapper;
    @Mock SupplierDeliveryResponseMapper responseMapper;
    @Mock SupplierDeliveryResponseEntryMapper responseEntryMapper;
    @Mock PurchaseOrderMapper orderMapper;
    @Mock PurchaseOrderEntryMapper orderEntryMapper;
    @Mock BusinessEventOutboxService outboxService;

    @Test
    void createShouldRequireFullPurchaseOrderCoverage() {
        PurchaseOrderEntity order = order();
        PurchaseOrderEntryEntity orderEntry = orderEntry();
        when(orderMapper.selectByIdForUpdate(100L, "tenant-a")).thenReturn(order);
        when(planMapper.selectCount(any())).thenReturn(0L);
        when(orderEntryMapper.selectList(any())).thenReturn(List.of(orderEntry));
        when(orderEntryMapper.selectByIdForUpdate(101L, "tenant-a"))
                .thenReturn(orderEntry);
        when(planMapper.insert(any())).thenReturn(1);
        when(planEntryMapper.insert(any())).thenReturn(1);

        Detail detail = service().create(new CreateRequest(
                "tenant-a",
                100L,
                "DP-001",
                LocalDate.of(2026, 8, 27),
                "首批交付计划",
                List.of(
                        new EntryRequest(
                                101L,
                                new BigDecimal("4"),
                                LocalDate.of(2026, 9, 10)
                        ),
                        new EntryRequest(
                                101L,
                                new BigDecimal("6"),
                                LocalDate.of(2026, 9, 20)
                        )
                )
        ), 99L);

        assertEquals("DRAFT", detail.plan().getFstatus());
        assertEquals(500L, detail.plan().getFbusinessPartnerId());
        assertEquals(2, detail.entries().size());
        assertEquals(
                new BigDecimal("10"),
                detail.entries().stream()
                        .map(PurchaseDeliveryPlanEntryEntity::getFplannedQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    @Test
    void createShouldRejectIncompleteQuantityCoverage() {
        when(orderMapper.selectByIdForUpdate(100L, "tenant-a"))
                .thenReturn(order());
        when(planMapper.selectCount(any())).thenReturn(0L);
        when(orderEntryMapper.selectList(any())).thenReturn(List.of(orderEntry()));
        when(orderEntryMapper.selectByIdForUpdate(101L, "tenant-a"))
                .thenReturn(orderEntry());

        assertThrows(BizException.class, () -> service().create(
                new CreateRequest(
                        "tenant-a",
                        100L,
                        "DP-002",
                        LocalDate.of(2026, 8, 27),
                        null,
                        List.of(new EntryRequest(
                                101L,
                                new BigDecimal("9"),
                                LocalDate.of(2026, 9, 10)))
                ),
                99L
        ));
    }

    @Test
    void publishShouldEmitBusinessEvent() {
        PurchaseDeliveryPlanEntity plan = plan("DRAFT");
        PurchaseDeliveryPlanEntryEntity entry = planEntry(701L, new BigDecimal("10"));
        when(planMapper.selectByIdForUpdate(700L, "tenant-a")).thenReturn(plan);
        when(orderMapper.selectByIdForUpdate(100L, "tenant-a"))
                .thenReturn(order());
        when(planEntryMapper.selectByPlanIdForUpdate(700L, "tenant-a"))
                .thenReturn(List.of(entry));
        when(planMapper.updateById(any())).thenReturn(1);

        Detail detail = service().publish(700L, "tenant-a", 99L);

        assertEquals("PUBLISHED", detail.plan().getFstatus());
        verify(outboxService).append(
                eq("tenant-a"),
                eq(1000L),
                eq("PURCHASE_DELIVERY_PLAN_PUBLISHED"),
                eq("PURCHASE_DELIVERY_PLAN"),
                eq(700L),
                anyLong(),
                eq("ERP_PURCHASE_DELIVERY_PLAN"),
                eq("DP-001"),
                eq(LocalDate.of(2026, 8, 27)),
                eq(99L),
                any()
        );
    }

    @Test
    void supplierConfirmShouldApplyOriginalPlanAndConfirmPlan() {
        PurchaseDeliveryPlanEntity plan = plan("PUBLISHED");
        PurchaseDeliveryPlanEntryEntity entry = planEntry(701L, new BigDecimal("10"));
        when(planMapper.selectByIdForUpdate(700L, "tenant-a")).thenReturn(plan);
        when(responseMapper.selectCount(any())).thenReturn(0L);
        when(planEntryMapper.selectByPlanIdForUpdate(700L, "tenant-a"))
                .thenReturn(List.of(entry));
        when(responseMapper.insert(any())).thenReturn(1);
        when(responseEntryMapper.insert(any())).thenReturn(1);
        when(planEntryMapper.updateById(any())).thenReturn(1);
        when(planMapper.updateById(any())).thenReturn(1);
        when(responseMapper.updateById(any())).thenReturn(1);

        ResponseDetail result = service().recordSupplierResponse(
                700L,
                new SupplierResponseRequest(
                        "tenant-a",
                        "DR-001",
                        LocalDate.of(2026, 8, 28),
                        "CONFIRM",
                        "按计划交付",
                        null
                ),
                99L
        );

        assertEquals("APPLIED", result.response().getFstatus());
        assertEquals("CONFIRMED", plan.getFstatus());
        assertEquals(new BigDecimal("10"), entry.getFcommittedQuantity());
        assertEquals(LocalDate.of(2026, 9, 10), entry.getFcommittedDeliveryDate());
        assertEquals("CONFIRMED", entry.getFresponseStatus());

        verify(outboxService, times(2)).append(
                anyString(), anyLong(), anyString(), anyString(),
                anyLong(), anyLong(), anyString(), anyString(),
                any(LocalDate.class), any(), any()
        );
    }

    @Test
    void supplierChangeShouldNotReduceTotalOrderCommitment() {
        PurchaseDeliveryPlanEntity plan = plan("CONFIRMED");
        PurchaseDeliveryPlanEntryEntity entry = planEntry(701L, new BigDecimal("10"));
        entry.setFcommittedQuantity(new BigDecimal("10"));
        entry.setFcommittedDeliveryDate(LocalDate.of(2026, 9, 10));

        when(planMapper.selectByIdForUpdate(700L, "tenant-a")).thenReturn(plan);
        when(orderEntryMapper.selectList(any())).thenReturn(List.of(orderEntry()));
        when(responseMapper.selectCount(any())).thenReturn(0L);
        when(planEntryMapper.selectByPlanIdForUpdate(700L, "tenant-a"))
                .thenReturn(List.of(entry));
        when(responseMapper.insert(any())).thenReturn(1);

        assertThrows(BizException.class, () -> service().recordSupplierResponse(
                700L,
                new SupplierResponseRequest(
                        "tenant-a",
                        "DR-002",
                        LocalDate.of(2026, 9, 1),
                        "CHANGE",
                        "只能交9件",
                        List.of(new SupplierResponseEntryRequest(
                                701L,
                                new BigDecimal("9"),
                                LocalDate.of(2026, 9, 25),
                                "产能不足"
                        ))
                ),
                99L
        ));
    }

    @Test
    void supplierRejectShouldKeepHistoryAndMovePlanToRejected() {
        PurchaseDeliveryPlanEntity plan = plan("PUBLISHED");
        PurchaseDeliveryPlanEntryEntity entry = planEntry(701L, new BigDecimal("10"));
        when(planMapper.selectByIdForUpdate(700L, "tenant-a")).thenReturn(plan);
        when(responseMapper.selectCount(any())).thenReturn(0L);
        when(planEntryMapper.selectByPlanIdForUpdate(700L, "tenant-a"))
                .thenReturn(List.of(entry));
        when(responseMapper.insert(any())).thenReturn(1);
        when(planEntryMapper.updateById(any())).thenReturn(1);
        when(planMapper.updateById(any())).thenReturn(1);

        ResponseDetail response = service().recordSupplierResponse(
                700L,
                new SupplierResponseRequest(
                        "tenant-a",
                        "DR-003",
                        LocalDate.of(2026, 8, 28),
                        "REJECT",
                        "无法满足交期",
                        null
                ),
                99L
        );

        assertEquals("REJECT", response.response().getFresponseType());
        assertEquals("REJECTED", plan.getFstatus());
        assertEquals("REJECTED", entry.getFresponseStatus());
    }

    private PurchaseDeliveryPlanService service() {
        return new PurchaseDeliveryPlanService(
                planMapper,
                planEntryMapper,
                responseMapper,
                responseEntryMapper,
                orderMapper,
                orderEntryMapper,
                outboxService
        );
    }

    private PurchaseOrderEntity order() {
        PurchaseOrderEntity value = new PurchaseOrderEntity();
        value.setFid(100L);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFnumber("PO-001");
        value.setFbusinessPartnerId(500L);
        value.setFbusinessPartnerCode("SUP-A");
        value.setFbusinessPartnerName("供应商A");
        value.setFcurrencyCode("CNY");
        value.setFstatus("EFFECTIVE");
        value.setFapprovalStatus("AUDITED");
        value.setFcloseStatus("OPEN");
        return value;
    }

    private PurchaseOrderEntryEntity orderEntry() {
        PurchaseOrderEntryEntity value = new PurchaseOrderEntryEntity();
        value.setFid(101L);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFpurchaseOrderId(100L);
        value.setFmaterialId(1L);
        value.setFmaterialCode("M001");
        value.setFmaterialName("物料1");
        value.setFquantity(new BigDecimal("10"));
        value.setFreceivedQuantity(BigDecimal.ZERO);
        value.setFreceiptReservedQuantity(BigDecimal.ZERO);
        value.setFversion(0);
        return value;
    }

    private PurchaseDeliveryPlanEntity plan(String status) {
        PurchaseDeliveryPlanEntity value = new PurchaseDeliveryPlanEntity();
        value.setFid(700L);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFnumber("DP-001");
        value.setFdate(LocalDate.of(2026, 8, 27));
        value.setFpurchaseOrderId(100L);
        value.setFpurchaseOrderNo("PO-001");
        value.setFbusinessPartnerId(500L);
        value.setFbusinessPartnerCode("SUP-A");
        value.setFbusinessPartnerName("供应商A");
        value.setFcurrencyCode("CNY");
        value.setFstatus(status);
        value.setFversion(0);
        return value;
    }

    private PurchaseDeliveryPlanEntryEntity planEntry(
            Long id, BigDecimal quantity
    ) {
        PurchaseDeliveryPlanEntryEntity value =
                new PurchaseDeliveryPlanEntryEntity();
        value.setFid(id);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFdeliveryPlanId(700L);
        value.setFpurchaseOrderId(100L);
        value.setFpurchaseOrderEntryId(101L);
        value.setFmaterialId(1L);
        value.setFmaterialCode("M001");
        value.setFmaterialName("物料1");
        value.setFplannedQuantity(quantity);
        value.setFplannedDeliveryDate(LocalDate.of(2026, 9, 10));
        value.setFresponseStatus("WAITING");
        value.setFreceivedQuantity(BigDecimal.ZERO);
        value.setFversion(0);
        return value;
    }
}
