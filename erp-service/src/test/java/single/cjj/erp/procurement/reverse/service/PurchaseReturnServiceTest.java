package single.cjj.erp.procurement.reverse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntryEntity;
import single.cjj.erp.procurement.inbound.mapper.PurchaseInboundEntryMapper;
import single.cjj.erp.procurement.inbound.mapper.PurchaseInboundMapper;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.reverse.entity.PurchaseReturnEntity;
import single.cjj.erp.procurement.reverse.entity.PurchaseReturnEntryEntity;
import single.cjj.erp.procurement.reverse.mapper.PurchaseReturnEntryMapper;
import single.cjj.erp.procurement.reverse.mapper.PurchaseReturnMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseReturnServiceTest {
    @Mock PurchaseReturnMapper returnMapper;
    @Mock PurchaseReturnEntryMapper returnEntryMapper;
    @Mock PurchaseInboundMapper inboundMapper;
    @Mock PurchaseInboundEntryMapper inboundEntryMapper;
    @Mock PurchaseOrderEntryMapper orderEntryMapper;
    @Mock BusinessEventOutboxService outboxService;

    @Test
    void confirmShouldPreserveInboundHistoryAndAccumulateReturnedQuantity() {
        PurchaseReturnEntity header=header();
        PurchaseReturnEntryEntity returnEntry=returnEntry(new BigDecimal("3"));
        PurchaseInboundEntryEntity inboundEntry=inboundEntry();
        PurchaseOrderEntryEntity orderEntry=orderEntry();

        when(returnMapper.selectByIdForUpdate(700L,"T1")).thenReturn(header);
        when(returnEntryMapper.selectList(any())).thenReturn(List.of(returnEntry));
        when(inboundEntryMapper.selectByIdForUpdate(201L,"T1")).thenReturn(inboundEntry);
        when(orderEntryMapper.selectByIdForUpdate(101L,"T1")).thenReturn(orderEntry);
        when(inboundEntryMapper.updateById(any())).thenReturn(1);
        when(orderEntryMapper.updateById(any())).thenReturn(1);
        when(returnMapper.updateById(any())).thenReturn(1);

        var result=service().confirm(700L,"T1",99L);

        assertEquals(new BigDecimal("3"),inboundEntry.getFreturnedQuantity());
        assertEquals(new BigDecimal("3"),orderEntry.getFreturnedQuantity());
        assertEquals(new BigDecimal("10"),inboundEntry.getFquantity());
        assertEquals("CONFIRMED",result.header().getFstatus());
        assertEquals("AUDITED",result.header().getFapprovalStatus());
        verify(outboxService).append(
                eq("T1"),eq(1L),eq("PURCHASE_RETURN_CONFIRMED"),
                eq("PURCHASE_RETURN"),eq(700L),anyLong(),
                eq("ERP_PURCHASE_RETURN"),eq("PRT-001"),
                eq(LocalDate.of(2026,8,27)),eq(99L),any());
    }

    @Test
    void confirmShouldRejectCumulativeOverReturn() {
        PurchaseReturnEntity header=header();
        PurchaseReturnEntryEntity returnEntry=returnEntry(new BigDecimal("3"));
        PurchaseInboundEntryEntity inboundEntry=inboundEntry();
        inboundEntry.setFreturnedQuantity(new BigDecimal("8"));

        when(returnMapper.selectByIdForUpdate(700L,"T1")).thenReturn(header);
        when(returnEntryMapper.selectList(any())).thenReturn(List.of(returnEntry));
        when(inboundEntryMapper.selectByIdForUpdate(201L,"T1")).thenReturn(inboundEntry);

        assertThrows(BizException.class,()->service().confirm(700L,"T1",99L));
        verify(orderEntryMapper,never()).updateById(any());
        verify(outboxService,never()).append(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
    }

    private PurchaseReturnService service(){
        return new PurchaseReturnService(returnMapper,returnEntryMapper,inboundMapper,inboundEntryMapper,orderEntryMapper,outboxService);
    }
    private PurchaseReturnEntity header(){
        PurchaseReturnEntity h=new PurchaseReturnEntity();
        h.setFid(700L);h.setFtenantId("T1");h.setForgId(1L);h.setFnumber("PRT-001");
        h.setFdate(LocalDate.of(2026,8,27));h.setFpurchaseInboundId(200L);h.setFpurchaseOrderId(100L);
        h.setFbusinessPartnerId(500L);h.setFbusinessPartnerCode("SUP-1");h.setFbusinessPartnerName("供应商1");
        h.setFcurrencyCode("CNY");h.setFtotalQuantity(new BigDecimal("3"));h.setFtotalAmount(new BigDecimal("300"));
        h.setFreasonType("QUALITY");h.setFstatus("DRAFT");h.setFapprovalStatus("SUBMITTED");h.setFversion(0);return h;
    }
    private PurchaseReturnEntryEntity returnEntry(BigDecimal qty){
        PurchaseReturnEntryEntity e=new PurchaseReturnEntryEntity();e.setFid(701L);e.setFpurchaseReturnId(700L);
        e.setFpurchaseInboundId(200L);e.setFpurchaseInboundEntryId(201L);e.setFpurchaseOrderId(100L);e.setFpurchaseOrderEntryId(101L);
        e.setFmaterialId(1L);e.setFmaterialCode("M001");e.setFquantity(qty);e.setFunitPrice(new BigDecimal("100"));e.setFamount(qty.multiply(new BigDecimal("100")));return e;
    }
    private PurchaseInboundEntryEntity inboundEntry(){
        PurchaseInboundEntryEntity e=new PurchaseInboundEntryEntity();e.setFid(201L);e.setFtenantId("T1");e.setFpurchaseInboundId(200L);
        e.setFpurchaseOrderId(100L);e.setFpurchaseOrderEntryId(101L);e.setFquantity(new BigDecimal("10"));e.setFreturnedQuantity(BigDecimal.ZERO);e.setFversion(0);return e;
    }
    private PurchaseOrderEntryEntity orderEntry(){
        PurchaseOrderEntryEntity e=new PurchaseOrderEntryEntity();e.setFid(101L);e.setFtenantId("T1");e.setFinboundQuantity(new BigDecimal("10"));
        e.setFreturnedQuantity(BigDecimal.ZERO);e.setFversion(0);return e;
    }
}
