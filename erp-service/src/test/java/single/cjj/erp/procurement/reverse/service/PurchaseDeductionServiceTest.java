package single.cjj.erp.procurement.reverse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.reverse.entity.*;
import single.cjj.erp.procurement.reverse.mapper.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseDeductionServiceTest {
    @Mock PurchaseDeductionMapper deductionMapper;
    @Mock PurchaseDeductionEntryMapper deductionEntryMapper;
    @Mock SupplierClaimMapper claimMapper;
    @Mock SupplierClaimEntryMapper claimEntryMapper;
    @Mock BusinessEventOutboxService outboxService;

    @Test
    void confirmShouldConsumeClaimBalanceAndEmitFiEvent() {
        PurchaseDeductionEntity deduction=deduction(new BigDecimal("50.00"));
        PurchaseDeductionEntryEntity deductionEntry=deductionEntry(new BigDecimal("50.00"));
        SupplierClaimEntity claim=claim();
        SupplierClaimEntryEntity claimEntry=claimEntry(new BigDecimal("20.00"));

        when(deductionMapper.selectByIdForUpdate(800L,"T1")).thenReturn(deduction);
        when(claimMapper.selectByIdForUpdate(700L,"T1")).thenReturn(claim);
        when(deductionEntryMapper.selectList(any())).thenReturn(List.of(deductionEntry));
        when(claimEntryMapper.selectByIdForUpdate(701L,"T1")).thenReturn(claimEntry);
        when(claimEntryMapper.updateById(any())).thenReturn(1);
        when(claimEntryMapper.selectList(any())).thenReturn(List.of(claimEntry));
        when(claimMapper.updateById(any())).thenReturn(1);
        when(deductionMapper.updateById(any())).thenReturn(1);

        var result=service().confirm(800L,"T1",99L);

        assertEquals(new BigDecimal("70.00"),claimEntry.getFdeductedAmount());
        assertEquals(new BigDecimal("70.00"),claim.getFdeductedAmount());
        assertEquals("PARTIAL",claim.getFdeductionStatus());
        assertEquals("CONFIRMED",result.header().getFstatus());
        verify(outboxService).append(
                eq("T1"),eq(1L),eq("PURCHASE_DEDUCTION_CONFIRMED"),eq("PURCHASE_DEDUCTION"),
                eq(800L),anyLong(),eq("ERP_PURCHASE_DEDUCTION"),eq("DED-001"),
                eq(LocalDate.of(2026,8,27)),eq(99L),any());
    }

    @Test
    void confirmShouldRejectCumulativeDeductionAboveAgreedAmount() {
        PurchaseDeductionEntity deduction=deduction(new BigDecimal("90.00"));
        PurchaseDeductionEntryEntity deductionEntry=deductionEntry(new BigDecimal("90.00"));
        SupplierClaimEntity claim=claim();
        SupplierClaimEntryEntity claimEntry=claimEntry(new BigDecimal("20.00"));

        when(deductionMapper.selectByIdForUpdate(800L,"T1")).thenReturn(deduction);
        when(claimMapper.selectByIdForUpdate(700L,"T1")).thenReturn(claim);
        when(deductionEntryMapper.selectList(any())).thenReturn(List.of(deductionEntry));
        when(claimEntryMapper.selectByIdForUpdate(701L,"T1")).thenReturn(claimEntry);

        assertThrows(BizException.class,()->service().confirm(800L,"T1",99L));
        verify(outboxService,never()).append(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
    }

    private PurchaseDeductionService service(){
        return new PurchaseDeductionService(deductionMapper,deductionEntryMapper,claimMapper,claimEntryMapper,outboxService);
    }
    private PurchaseDeductionEntity deduction(BigDecimal amount){
        PurchaseDeductionEntity h=new PurchaseDeductionEntity();h.setFid(800L);h.setFtenantId("T1");h.setForgId(1L);h.setFnumber("DED-001");
        h.setFdate(LocalDate.of(2026,8,27));h.setFsupplierClaimId(700L);h.setFpurchaseOrderId(100L);h.setFbusinessPartnerId(500L);
        h.setFcurrencyCode("CNY");h.setFamount(amount);h.setFstatus("DRAFT");h.setFapprovalStatus("SUBMITTED");h.setFversion(0);return h;
    }
    private PurchaseDeductionEntryEntity deductionEntry(BigDecimal amount){
        PurchaseDeductionEntryEntity e=new PurchaseDeductionEntryEntity();e.setFid(801L);e.setFpurchaseDeductionId(800L);e.setFsupplierClaimEntryId(701L);
        e.setFpurchaseOrderId(100L);e.setFpurchaseOrderEntryId(101L);e.setFmaterialId(1L);e.setFmaterialCode("M001");e.setFamount(amount);return e;
    }
    private SupplierClaimEntity claim(){
        SupplierClaimEntity h=new SupplierClaimEntity();h.setFid(700L);h.setFtenantId("T1");h.setForgId(1L);h.setFnumber("CLM-001");
        h.setFstatus("CONFIRMED");h.setFapprovalStatus("AUDITED");h.setFagreedAmount(new BigDecimal("100.00"));
        h.setFdeductedAmount(new BigDecimal("20.00"));h.setFdeductionStatus("PARTIAL");h.setFversion(0);return h;
    }
    private SupplierClaimEntryEntity claimEntry(BigDecimal deducted){
        SupplierClaimEntryEntity e=new SupplierClaimEntryEntity();e.setFid(701L);e.setFsupplierClaimId(700L);e.setFpurchaseOrderId(100L);e.setFpurchaseOrderEntryId(101L);
        e.setFagreedAmount(new BigDecimal("100.00"));e.setFdeductedAmount(deducted);e.setFversion(0);return e;
    }
}
