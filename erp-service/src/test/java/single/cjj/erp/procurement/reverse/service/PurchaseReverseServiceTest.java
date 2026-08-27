package single.cjj.erp.procurement.reverse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntity;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntryEntity;
import single.cjj.erp.procurement.inbound.mapper.PurchaseInboundEntryMapper;
import single.cjj.erp.procurement.inbound.mapper.PurchaseInboundMapper;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;
import single.cjj.erp.procurement.reverse.dto.PurchaseReverseContracts.*;
import single.cjj.erp.procurement.reverse.entity.*;
import single.cjj.erp.procurement.reverse.mapper.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseReverseServiceTest {

    @Mock PurchaseReturnMapper returnMapper;
    @Mock PurchaseReturnEntryMapper returnEntryMapper;
    @Mock PurchaseInboundMapper inboundMapper;
    @Mock PurchaseInboundEntryMapper inboundEntryMapper;
    @Mock PurchaseOrderMapper orderMapper;
    @Mock PurchaseOrderEntryMapper orderEntryMapper;
    @Mock SupplierClaimMapper claimMapper;
    @Mock SupplierClaimEntryMapper claimEntryMapper;
    @Mock PurchaseDeductionMapper deductionMapper;
    @Mock PurchaseDeductionEntryMapper deductionEntryMapper;
    @Mock BusinessEventOutboxService outboxService;

    @Test
    void returnCreateShouldRejectQuantityBeyondInboundRemaining() {
        PurchaseInboundEntity inbound = inbound();
        PurchaseInboundEntryEntity inboundEntry = inboundEntry();
        inboundEntry.setFreturnedQuantity(new BigDecimal("8"));

        when(inboundMapper.selectByIdForUpdate(100L, "tenant-a")).thenReturn(inbound);
        when(returnMapper.selectCount(any())).thenReturn(0L);
        when(inboundEntryMapper.selectByIdForUpdate(101L, "tenant-a")).thenReturn(inboundEntry);

        assertThrows(BizException.class, () -> returnService().create(
                new ReturnCreateRequest(
                        "tenant-a", 100L, "PRT-001", LocalDate.of(2026, 8, 27),
                        "QUALITY", "质量问题",
                        List.of(new ReturnEntryRequest(101L, new BigDecimal("3")))
                ),
                99L
        ));
    }

    @Test
    void returnConfirmShouldWriteBackInboundAndOrderReturnedQuantityAndEmitEvent() {
        PurchaseReturnEntity header = purchaseReturn();
        PurchaseReturnEntryEntity returnEntry = returnEntry();
        PurchaseInboundEntryEntity inboundEntry = inboundEntry();
        PurchaseOrderEntryEntity orderEntry = orderEntry();

        when(returnMapper.selectByIdForUpdate(700L, "tenant-a")).thenReturn(header);
        when(returnEntryMapper.selectList(any())).thenReturn(List.of(returnEntry));
        when(inboundEntryMapper.selectByIdForUpdate(101L, "tenant-a")).thenReturn(inboundEntry);
        when(inboundEntryMapper.updateById(any())).thenReturn(1);
        when(orderEntryMapper.selectByIdForUpdate(201L, "tenant-a")).thenReturn(orderEntry);
        when(orderEntryMapper.updateById(any())).thenReturn(1);
        when(returnMapper.updateById(any())).thenReturn(1);

        ReturnDetail detail = returnService().confirm(700L, "tenant-a", 99L);

        assertEquals(new BigDecimal("4"), inboundEntry.getFreturnedQuantity());
        assertEquals(new BigDecimal("4"), orderEntry.getFreturnedQuantity());
        assertEquals("CONFIRMED", detail.header().getFstatus());
        assertEquals("AUDITED", detail.header().getFapprovalStatus());
        verify(outboxService).append(
                eq("tenant-a"), eq(1000L), eq("PURCHASE_RETURN_CONFIRMED"),
                eq("PURCHASE_RETURN"), eq(700L), anyLong(),
                eq("ERP_PURCHASE_RETURN"), eq("PRT-001"),
                eq(LocalDate.of(2026, 8, 27)), eq(99L), any());
    }

    @Test
    void claimConfirmShouldRejectAgreedAmountBeyondRequestedAmount() {
        SupplierClaimEntity claim = supplierClaim();
        SupplierClaimEntryEntity entry = claimEntry();

        when(claimMapper.selectByIdForUpdate(800L, "tenant-a")).thenReturn(claim);
        when(claimEntryMapper.selectList(any())).thenReturn(List.of(entry));
        when(claimEntryMapper.selectByIdForUpdate(801L, "tenant-a")).thenReturn(entry);

        assertThrows(BizException.class, () -> claimService().confirm(
                800L,
                new ClaimConfirmRequest(
                        "tenant-a",
                        List.of(new ClaimAgreeEntryRequest(801L, new BigDecimal("101.00")))
                ),
                99L
        ));
    }

    @Test
    void deductionCreateShouldRejectAmountBeyondClaimRemaining() {
        SupplierClaimEntity claim = supplierClaim();
        claim.setFstatus("CONFIRMED");
        claim.setFapprovalStatus("AUDITED");
        SupplierClaimEntryEntity entry = claimEntry();
        entry.setFagreedAmount(new BigDecimal("100.00"));
        entry.setFdeductedAmount(new BigDecimal("80.00"));

        when(claimMapper.selectByIdForUpdate(800L, "tenant-a")).thenReturn(claim);
        when(deductionMapper.selectCount(any())).thenReturn(0L);
        when(claimEntryMapper.selectByIdForUpdate(801L, "tenant-a")).thenReturn(entry);

        assertThrows(BizException.class, () -> deductionService().create(
                new DeductionCreateRequest(
                        "tenant-a", 800L, "DED-001", LocalDate.of(2026, 8, 27),
                        "应付扣款", List.of(new DeductionEntryRequest(801L, new BigDecimal("21.00")))
                ),
                99L
        ));
    }

    @Test
    void deductionConfirmShouldUpdateClaimDeductedAmountStatusAndEmitEvent() {
        PurchaseDeductionEntity deduction = deduction();
        PurchaseDeductionEntryEntity deductionEntry = deductionEntry();
        SupplierClaimEntity claim = supplierClaim();
        claim.setFstatus("CONFIRMED");
        claim.setFapprovalStatus("AUDITED");
        claim.setFagreedAmount(new BigDecimal("100.00"));
        claim.setFdeductedAmount(new BigDecimal("40.00"));
        claim.setFdeductionStatus("PARTIAL");
        SupplierClaimEntryEntity claimEntry = claimEntry();
        claimEntry.setFagreedAmount(new BigDecimal("100.00"));
        claimEntry.setFdeductedAmount(new BigDecimal("40.00"));

        when(deductionMapper.selectByIdForUpdate(900L, "tenant-a")).thenReturn(deduction);
        when(claimMapper.selectByIdForUpdate(800L, "tenant-a")).thenReturn(claim);
        when(deductionEntryMapper.selectList(any())).thenReturn(List.of(deductionEntry));
        when(claimEntryMapper.selectByIdForUpdate(801L, "tenant-a")).thenReturn(claimEntry);
        when(claimEntryMapper.updateById(any())).thenReturn(1);
        when(claimEntryMapper.selectList(any())).thenReturn(List.of(claimEntry));
        when(claimMapper.updateById(any())).thenReturn(1);
        when(deductionMapper.updateById(any())).thenReturn(1);

        DeductionDetail detail = deductionService().confirm(900L, "tenant-a", 99L);

        assertEquals(new BigDecimal("100.00"), claimEntry.getFdeductedAmount());
        assertEquals(new BigDecimal("100.00"), claim.getFdeductedAmount());
        assertEquals("COMPLETE", claim.getFdeductionStatus());
        assertEquals("CONFIRMED", detail.header().getFstatus());
        assertEquals("AUDITED", detail.header().getFapprovalStatus());
        verify(outboxService).append(
                eq("tenant-a"), eq(1000L), eq("PURCHASE_DEDUCTION_CONFIRMED"),
                eq("PURCHASE_DEDUCTION"), eq(900L), anyLong(),
                eq("ERP_PURCHASE_DEDUCTION"), eq("DED-001"),
                eq(LocalDate.of(2026, 8, 27)), eq(99L), any());
    }

    private PurchaseReturnService returnService() {
        return new PurchaseReturnService(
                returnMapper, returnEntryMapper, inboundMapper, inboundEntryMapper,
                orderEntryMapper, outboxService);
    }

    private SupplierClaimService claimService() {
        return new SupplierClaimService(
                claimMapper, claimEntryMapper, orderMapper, orderEntryMapper,
                returnMapper, returnEntryMapper, outboxService);
    }

    private PurchaseDeductionService deductionService() {
        return new PurchaseDeductionService(
                deductionMapper, deductionEntryMapper,
                claimMapper, claimEntryMapper, outboxService);
    }

    private PurchaseInboundEntity inbound() {
        PurchaseInboundEntity v = new PurchaseInboundEntity();
        v.setFid(100L); v.setFtenantId("tenant-a"); v.setForgId(1000L);
        v.setFnumber("PIN-001"); v.setFdate(LocalDate.of(2026, 8, 20));
        v.setFbusinessPartnerId(500L); v.setFbusinessPartnerCode("SUP-A");
        v.setFbusinessPartnerName("供应商A"); v.setFcurrencyCode("CNY");
        v.setFwarehouseId(10L); v.setFstatus("CONFIRMED"); v.setFapprovalStatus("AUDITED");
        v.setFversion(0); return v;
    }

    private PurchaseInboundEntryEntity inboundEntry() {
        PurchaseInboundEntryEntity v = new PurchaseInboundEntryEntity();
        v.setFid(101L); v.setFtenantId("tenant-a"); v.setForgId(1000L);
        v.setFpurchaseInboundId(100L); v.setFpurchaseOrderId(200L); v.setFpurchaseOrderEntryId(201L);
        v.setFmaterialId(1L); v.setFmaterialCode("M001"); v.setFmaterialName("物料1");
        v.setFquantity(new BigDecimal("10")); v.setFreturnedQuantity(BigDecimal.ZERO);
        v.setFunitPrice(new BigDecimal("20")); v.setFwarehouseId(10L); v.setFversion(0); return v;
    }

    private PurchaseOrderEntryEntity orderEntry() {
        PurchaseOrderEntryEntity v = new PurchaseOrderEntryEntity();
        v.setFid(201L); v.setFtenantId("tenant-a"); v.setFpurchaseOrderId(200L);
        v.setFinboundQuantity(new BigDecimal("10")); v.setFreturnedQuantity(BigDecimal.ZERO); v.setFversion(0); return v;
    }

    private PurchaseReturnEntity purchaseReturn() {
        PurchaseReturnEntity v = new PurchaseReturnEntity();
        v.setFid(700L); v.setFtenantId("tenant-a"); v.setForgId(1000L);
        v.setFnumber("PRT-001"); v.setFdate(LocalDate.of(2026, 8, 27));
        v.setFpurchaseInboundId(100L); v.setFpurchaseOrderId(200L);
        v.setFstatus("DRAFT"); v.setFapprovalStatus("SUBMITTED"); v.setFversion(0); return v;
    }

    private PurchaseReturnEntryEntity returnEntry() {
        PurchaseReturnEntryEntity v = new PurchaseReturnEntryEntity();
        v.setFid(701L); v.setFpurchaseReturnId(700L); v.setFpurchaseInboundEntryId(101L);
        v.setFpurchaseOrderEntryId(201L); v.setFquantity(new BigDecimal("4")); v.setFversion(0); return v;
    }

    private SupplierClaimEntity supplierClaim() {
        SupplierClaimEntity v = new SupplierClaimEntity();
        v.setFid(800L); v.setFtenantId("tenant-a"); v.setForgId(1000L);
        v.setFnumber("CLM-001"); v.setFdate(LocalDate.of(2026, 8, 27));
        v.setFpurchaseOrderId(200L); v.setFbusinessPartnerId(500L);
        v.setFbusinessPartnerCode("SUP-A"); v.setFbusinessPartnerName("供应商A");
        v.setFcurrencyCode("CNY"); v.setFrequestedAmount(new BigDecimal("100.00"));
        v.setFagreedAmount(BigDecimal.ZERO.setScale(2)); v.setFdeductedAmount(BigDecimal.ZERO.setScale(2));
        v.setFstatus("DRAFT"); v.setFapprovalStatus("SUBMITTED"); v.setFversion(0); return v;
    }

    private SupplierClaimEntryEntity claimEntry() {
        SupplierClaimEntryEntity v = new SupplierClaimEntryEntity();
        v.setFid(801L); v.setFsupplierClaimId(800L); v.setFpurchaseOrderId(200L);
        v.setFpurchaseOrderEntryId(201L); v.setFmaterialId(1L); v.setFmaterialCode("M001");
        v.setFmaterialName("物料1"); v.setFrequestedAmount(new BigDecimal("100.00"));
        v.setFagreedAmount(BigDecimal.ZERO.setScale(2)); v.setFdeductedAmount(BigDecimal.ZERO.setScale(2));
        v.setFversion(0); return v;
    }

    private PurchaseDeductionEntity deduction() {
        PurchaseDeductionEntity v = new PurchaseDeductionEntity();
        v.setFid(900L); v.setFtenantId("tenant-a"); v.setForgId(1000L);
        v.setFnumber("DED-001"); v.setFdate(LocalDate.of(2026, 8, 27));
        v.setFsupplierClaimId(800L); v.setFpurchaseOrderId(200L);
        v.setFbusinessPartnerId(500L); v.setFbusinessPartnerCode("SUP-A");
        v.setFbusinessPartnerName("供应商A"); v.setFcurrencyCode("CNY");
        v.setFamount(new BigDecimal("60.00")); v.setFstatus("DRAFT");
        v.setFapprovalStatus("SUBMITTED"); v.setFversion(0); return v;
    }

    private PurchaseDeductionEntryEntity deductionEntry() {
        PurchaseDeductionEntryEntity v = new PurchaseDeductionEntryEntity();
        v.setFid(901L); v.setFpurchaseDeductionId(900L); v.setFsupplierClaimEntryId(801L);
        v.setFpurchaseOrderId(200L); v.setFpurchaseOrderEntryId(201L);
        v.setFmaterialId(1L); v.setFmaterialCode("M001"); v.setFmaterialName("物料1");
        v.setFamount(new BigDecimal("60.00")); v.setFversion(0); return v;
    }
}
