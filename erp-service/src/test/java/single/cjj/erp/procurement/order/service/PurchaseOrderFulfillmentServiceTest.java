package single.cjj.erp.procurement.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderFulfillmentServiceTest {

    @Mock
    private PurchaseOrderMapper orderMapper;
    @Mock
    private PurchaseOrderEntryMapper entryMapper;

    @Test
    void shouldReserveReceiptQuantityAndRejectOverReservation() {
        PurchaseOrderEntity order = order();
        PurchaseOrderEntryEntity entry = entry();
        when(entryMapper.selectByIdForUpdate(11L, "tenant-a")).thenReturn(entry);
        when(orderMapper.selectByIdForUpdate(1L, "tenant-a")).thenReturn(order);
        when(entryMapper.updateById(any())).thenReturn(1);

        PurchaseOrderFulfillmentService service = new PurchaseOrderFulfillmentService(orderMapper, entryMapper);
        var reserved = service.reserveReceipt(
                "tenant-a", 11L, new BigDecimal("30"), 9001L, 100L, 88L);

        assertEquals(new BigDecimal("30"), entry.getFreceiptReservedQuantity());
        assertEquals(new BigDecimal("10"), reserved.remainingAfterReservation());
        assertThrows(BizException.class, () -> service.reserveReceipt(
                "tenant-a", 11L, new BigDecimal("11"), 9001L, 100L, 88L));
    }

    @Test
    void shouldConvertReservationToReceivedQuantity() {
        PurchaseOrderEntity order = order();
        PurchaseOrderEntryEntity entry = entry();
        entry.setFreceiptReservedQuantity(new BigDecimal("30"));
        when(entryMapper.selectByIdForUpdate(11L, "tenant-a")).thenReturn(entry);
        when(orderMapper.selectByIdForUpdate(1L, "tenant-a")).thenReturn(order);
        when(entryMapper.updateById(any())).thenReturn(1);
        when(entryMapper.selectList(any())).thenReturn(java.util.List.of(entry));
        when(orderMapper.updateById(any())).thenReturn(1);

        PurchaseOrderFulfillmentService service = new PurchaseOrderFulfillmentService(orderMapper, entryMapper);
        service.confirmReceipt("tenant-a", 11L, new BigDecimal("30"), 88L);

        assertEquals(BigDecimal.ZERO, entry.getFreceiptReservedQuantity());
        assertEquals(new BigDecimal("90"), entry.getFreceivedQuantity());
        assertEquals("PARTIAL", order.getFreceiptStatus());
    }

    private PurchaseOrderEntity order() {
        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setFid(1L);
        order.setFtenantId("tenant-a");
        order.setForgId(100L);
        order.setFbusinessPartnerId(9001L);
        order.setFstatus("EFFECTIVE");
        order.setFapprovalStatus("AUDITED");
        order.setFreceiptStatus("NONE");
        order.setFversion(0);
        return order;
    }

    private PurchaseOrderEntryEntity entry() {
        PurchaseOrderEntryEntity entry = new PurchaseOrderEntryEntity();
        entry.setFid(11L);
        entry.setFtenantId("tenant-a");
        entry.setFpurchaseOrderId(1L);
        entry.setFquantity(new BigDecimal("100"));
        entry.setFreceivedQuantity(new BigDecimal("60"));
        entry.setFreceiptReservedQuantity(BigDecimal.ZERO);
        entry.setFversion(0);
        return entry;
    }
}
