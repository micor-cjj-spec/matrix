package single.cjj.erp.procurement.delivery.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.procurement.delivery.entity.PurchaseDeliveryPlanEntity;
import single.cjj.erp.procurement.delivery.entity.PurchaseDeliveryPlanEntryEntity;
import single.cjj.erp.procurement.delivery.mapper.PurchaseDeliveryPlanEntryMapper;
import single.cjj.erp.procurement.delivery.mapper.PurchaseDeliveryPlanMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryPlanFulfillmentServiceTest {

    @Mock PurchaseDeliveryPlanMapper planMapper;
    @Mock PurchaseDeliveryPlanEntryMapper entryMapper;

    @Test
    void reservationShouldRejectQuantityBeyondConfirmedCommitment() {
        PurchaseDeliveryPlanEntity plan = plan("CONFIRMED");
        PurchaseDeliveryPlanEntryEntity entry = entry(
                701L, new BigDecimal("10"), new BigDecimal("2"));

        when(planMapper.selectActiveByOrderEntryForUpdate(
                "tenant-a", 101L)).thenReturn(plan);
        when(entryMapper.selectByOrderEntryForUpdate(
                700L, "tenant-a", 101L)).thenReturn(List.of(entry));

        assertThrows(BizException.class, () -> service().validateReceiptReservation(
                "tenant-a",
                101L,
                new BigDecimal("6"),
                new BigDecimal("3")
        ));
    }

    @Test
    void receiptShouldAllocateByCommitmentOrderAndCompletePlan() {
        PurchaseDeliveryPlanEntity plan = plan("CONFIRMED");
        PurchaseDeliveryPlanEntryEntity first =
                entry(701L, new BigDecimal("4"), BigDecimal.ZERO);
        first.setFcommittedDeliveryDate(LocalDate.of(2026, 9, 10));
        PurchaseDeliveryPlanEntryEntity second =
                entry(702L, new BigDecimal("6"), BigDecimal.ZERO);
        second.setFcommittedDeliveryDate(LocalDate.of(2026, 9, 20));

        when(planMapper.selectActiveByOrderEntryForUpdate(
                "tenant-a", 101L)).thenReturn(plan);
        when(entryMapper.selectByOrderEntryForUpdate(
                700L, "tenant-a", 101L))
                .thenReturn(List.of(first, second));
        when(entryMapper.updateById(any())).thenReturn(1);
        when(entryMapper.selectByPlanIdForUpdate(
                700L, "tenant-a")).thenReturn(List.of(first, second));
        when(planMapper.updateById(any())).thenReturn(1);

        service().confirmReceipt(
                "tenant-a", 101L, new BigDecimal("10"), 99L);

        assertEquals(new BigDecimal("4"), first.getFreceivedQuantity());
        assertEquals(new BigDecimal("6"), second.getFreceivedQuantity());
        assertEquals("COMPLETE", plan.getFstatus());
    }

    @Test
    void legacyOrderWithoutDeliveryPlanShouldRemainReceivable() {
        when(planMapper.selectActiveByOrderEntryForUpdate(
                "tenant-a", 101L)).thenReturn(null);

        service().validateReceiptReservation(
                "tenant-a", 101L, new BigDecimal("10"), BigDecimal.ZERO);
        service().confirmReceipt(
                "tenant-a", 101L, new BigDecimal("10"), 99L);
    }

    private DeliveryPlanFulfillmentService service() {
        return new DeliveryPlanFulfillmentService(planMapper, entryMapper);
    }

    private PurchaseDeliveryPlanEntity plan(String status) {
        PurchaseDeliveryPlanEntity value = new PurchaseDeliveryPlanEntity();
        value.setFid(700L);
        value.setFtenantId("tenant-a");
        value.setFstatus(status);
        value.setFversion(0);
        return value;
    }

    private PurchaseDeliveryPlanEntryEntity entry(
            Long id, BigDecimal committed, BigDecimal received
    ) {
        PurchaseDeliveryPlanEntryEntity value =
                new PurchaseDeliveryPlanEntryEntity();
        value.setFid(id);
        value.setFdeliveryPlanId(700L);
        value.setFpurchaseOrderEntryId(101L);
        value.setFcommittedQuantity(committed);
        value.setFcommittedDeliveryDate(LocalDate.of(2026, 9, 10));
        value.setFreceivedQuantity(received);
        value.setFversion(0);
        return value;
    }
}
