package single.cjj.erp.procurement.reverse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;
import single.cjj.erp.procurement.reverse.dto.PurchaseReverseContracts.*;
import single.cjj.erp.procurement.reverse.entity.SupplierClaimEntity;
import single.cjj.erp.procurement.reverse.entity.SupplierClaimEntryEntity;
import single.cjj.erp.procurement.reverse.mapper.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierClaimServiceTest {
    @Mock SupplierClaimMapper claimMapper;
    @Mock SupplierClaimEntryMapper claimEntryMapper;
    @Mock PurchaseOrderMapper orderMapper;
    @Mock PurchaseOrderEntryMapper orderEntryMapper;
    @Mock PurchaseReturnMapper returnMapper;
    @Mock PurchaseReturnEntryMapper returnEntryMapper;
    @Mock BusinessEventOutboxService outboxService;

    @Test
    void confirmShouldPersistNegotiatedAmountAndEmitEvent() {
        SupplierClaimEntity header=header();
        SupplierClaimEntryEntity entry=entry();
        when(claimMapper.selectByIdForUpdate(700L,"T1")).thenReturn(header);
        when(claimEntryMapper.selectList(any())).thenReturn(List.of(entry));
        when(claimEntryMapper.selectByIdForUpdate(701L,"T1")).thenReturn(entry);
        when(claimEntryMapper.updateById(any())).thenReturn(1);
        when(claimMapper.updateById(any())).thenReturn(1);

        var result=service().confirm(700L,new ClaimConfirmRequest(
                "T1",List.of(new ClaimAgreeEntryRequest(701L,new BigDecimal("80")))),99L);

        assertEquals(new BigDecimal("80.00"),entry.getFagreedAmount());
        assertEquals(new BigDecimal("80.00"),result.header().getFagreedAmount());
        assertEquals("CONFIRMED",result.header().getFstatus());
        verify(outboxService).append(
                eq("T1"),eq(1L),eq("PURCHASE_CLAIM_CONFIRMED"),eq("SUPPLIER_CLAIM"),
                eq(700L),anyLong(),eq("ERP_SUPPLIER_CLAIM"),eq("CLM-001"),
                eq(LocalDate.of(2026,8,27)),eq(99L),any());
    }

    @Test
    void confirmShouldRejectAgreedAmountAboveRequestedAmount() {
        SupplierClaimEntity header=header();
        SupplierClaimEntryEntity entry=entry();
        when(claimMapper.selectByIdForUpdate(700L,"T1")).thenReturn(header);
        when(claimEntryMapper.selectList(any())).thenReturn(List.of(entry));
        when(claimEntryMapper.selectByIdForUpdate(701L,"T1")).thenReturn(entry);

        assertThrows(BizException.class,()->service().confirm(700L,new ClaimConfirmRequest(
                "T1",List.of(new ClaimAgreeEntryRequest(701L,new BigDecimal("101")))),99L));
        verify(outboxService,never()).append(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
    }

    private SupplierClaimService service(){
        return new SupplierClaimService(claimMapper,claimEntryMapper,orderMapper,orderEntryMapper,returnMapper,returnEntryMapper,outboxService);
    }
    private SupplierClaimEntity header(){
        SupplierClaimEntity h=new SupplierClaimEntity();h.setFid(700L);h.setFtenantId("T1");h.setForgId(1L);h.setFnumber("CLM-001");
        h.setFdate(LocalDate.of(2026,8,27));h.setFpurchaseOrderId(100L);h.setFbusinessPartnerId(500L);h.setFcurrencyCode("CNY");
        h.setFrequestedAmount(new BigDecimal("100.00"));h.setFagreedAmount(BigDecimal.ZERO);h.setFstatus("DRAFT");h.setFapprovalStatus("SUBMITTED");h.setFversion(0);return h;
    }
    private SupplierClaimEntryEntity entry(){
        SupplierClaimEntryEntity e=new SupplierClaimEntryEntity();e.setFid(701L);e.setFsupplierClaimId(700L);e.setFpurchaseOrderEntryId(101L);
        e.setFrequestedAmount(new BigDecimal("100.00"));e.setFagreedAmount(BigDecimal.ZERO);e.setFdeductedAmount(BigDecimal.ZERO);return e;
    }
}
