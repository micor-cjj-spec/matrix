package single.cjj.erp.procurement.contract.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.contract.dto.PurchaseContractContracts.*;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntity;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntryEntity;
import single.cjj.erp.procurement.contract.mapper.PurchaseContractEntryMapper;
import single.cjj.erp.procurement.contract.mapper.PurchaseContractMapper;
import single.cjj.erp.procurement.sourcing.entity.*;
import single.cjj.erp.procurement.sourcing.mapper.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseContractServiceTest {

    @Mock PurchaseContractMapper contractMapper;
    @Mock PurchaseContractEntryMapper contractEntryMapper;
    @Mock SourcingAwardMapper awardMapper;
    @Mock SourcingAwardEntryMapper awardEntryMapper;
    @Mock ProcurementRfqMapper rfqMapper;
    @Mock ProcurementRfqEntryMapper rfqEntryMapper;
    @Mock SupplierQuoteEntryMapper quoteEntryMapper;
    @Mock BusinessEventOutboxService outboxService;

    @Test
    void createShouldCalculateAmountsFromAwardSnapshot() {
        when(awardMapper.selectOne(any())).thenReturn(award());
        when(rfqMapper.selectOne(any())).thenReturn(rfq());
        when(contractMapper.selectCount(any())).thenReturn(0L);
        when(awardEntryMapper.selectByIdForUpdate(301L, "tenant-a")).thenReturn(awardEntry(301L, 500L, "SUP-A", "供应商A"));
        when(contractEntryMapper.selectList(any())).thenReturn(List.of());
        when(rfqEntryMapper.selectById(101L)).thenReturn(rfqEntry(101L, 11L));
        when(quoteEntryMapper.selectById(201L)).thenReturn(quoteEntry());
        when(contractMapper.insert(any())).thenReturn(1);
        when(contractEntryMapper.insert(any())).thenReturn(1);

        Detail detail = service().create(new CreateRequest(
                "tenant-a", 1000L, 900L, "PC-001", LocalDate.of(2026, 8, 27),
                "供应商A采购合同", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                "NET30", "DAP", null,
                List.of(new EntryRequest(301L, new BigDecimal("10"), null))
        ), 99L);

        assertEquals(500L, detail.contract().getFbusinessPartnerId());
        assertEquals("CNY", detail.contract().getFcurrencyCode());
        assertEquals(new BigDecimal("10"), detail.contract().getFtotalQuantity());
        assertEquals(new BigDecimal("1000.00"), detail.contract().getFnetAmount());
        assertEquals(new BigDecimal("130.00"), detail.contract().getFtaxAmount());
        assertEquals(new BigDecimal("1130.00"), detail.contract().getFgrossAmount());
        assertEquals(LocalDate.of(2026, 9, 15), detail.entries().get(0).getFplannedDeliveryDate());
        assertEquals(BigDecimal.ZERO, detail.entries().get(0).getForderedQuantity());
    }

    @Test
    void createShouldRejectAwardEntriesFromDifferentSuppliers() {
        when(awardMapper.selectOne(any())).thenReturn(award());
        when(rfqMapper.selectOne(any())).thenReturn(rfq());
        when(contractMapper.selectCount(any())).thenReturn(0L);
        when(awardEntryMapper.selectByIdForUpdate(301L, "tenant-a"))
                .thenReturn(awardEntry(301L, 500L, "SUP-A", "供应商A"));
        when(awardEntryMapper.selectByIdForUpdate(302L, "tenant-a"))
                .thenReturn(awardEntry(302L, 501L, "SUP-B", "供应商B"));
        when(contractEntryMapper.selectList(any())).thenReturn(List.of());
        when(rfqEntryMapper.selectById(101L)).thenReturn(rfqEntry(101L, 11L));
        when(quoteEntryMapper.selectById(201L)).thenReturn(quoteEntry());

        assertThrows(BizException.class, () -> service().create(new CreateRequest(
                "tenant-a", 1000L, 900L, "PC-002", LocalDate.of(2026, 8, 27),
                null, null, null, null, null, null,
                List.of(
                        new EntryRequest(301L, new BigDecimal("5"), null),
                        new EntryRequest(302L, new BigDecimal("5"), null)
                )
        ), 99L));
    }

    @Test
    void createShouldRejectQuantityBeyondUncontractedAwardQuantity() {
        when(awardMapper.selectOne(any())).thenReturn(award());
        when(rfqMapper.selectOne(any())).thenReturn(rfq());
        when(contractMapper.selectCount(any())).thenReturn(0L);
        when(awardEntryMapper.selectByIdForUpdate(301L, "tenant-a"))
                .thenReturn(awardEntry(301L, 500L, "SUP-A", "供应商A"));

        PurchaseContractEntryEntity existing = new PurchaseContractEntryEntity();
        existing.setFpurchaseContractId(700L);
        existing.setFquantity(new BigDecimal("7"));
        when(contractEntryMapper.selectList(any())).thenReturn(List.of(existing));

        assertThrows(BizException.class, () -> service().create(new CreateRequest(
                "tenant-a", 1000L, 900L, "PC-003", LocalDate.of(2026, 8, 27),
                null, null, null, null, null, null,
                List.of(new EntryRequest(301L, new BigDecimal("4"), null))
        ), 99L));
    }

    @Test
    void approvedContractShouldBecomeEffectiveAndEmitBusinessEvent() {
        PurchaseContractEntity contract = contract("SUBMITTED", "DRAFT");
        PurchaseContractEntryEntity entry = new PurchaseContractEntryEntity();
        entry.setFid(801L);
        entry.setFsourcingAwardEntryId(301L);
        entry.setFpurchaseRequestId(1L);
        entry.setFpurchaseRequestEntryId(11L);
        entry.setFmaterialId(100L);
        entry.setFmaterialCode("M001");
        entry.setFquantity(new BigDecimal("10"));
        entry.setFunitPrice(new BigDecimal("100"));
        entry.setFtaxRate(new BigDecimal("0.13"));
        entry.setFgrossAmount(new BigDecimal("1130.00"));

        when(contractMapper.selectByIdForUpdate(800L, "tenant-a")).thenReturn(contract);
        when(contractMapper.updateById(any())).thenReturn(1);
        when(contractEntryMapper.selectList(any())).thenReturn(List.of(entry));

        Detail detail = service().applyApprovalResult(
                800L,
                "tenant-a",
                new ApprovalResultRequest("APPROVED", "WF-PC-001", 99L, null)
        );

        assertEquals("EFFECTIVE", detail.contract().getFstatus());
        assertEquals("APPROVED", detail.contract().getFapprovalStatus());
        assertEquals("WF-PC-001", detail.contract().getFworkflowInstanceId());

        verify(outboxService).append(
                eq("tenant-a"),
                eq(1000L),
                eq("PURCHASE_CONTRACT_EFFECTIVE"),
                eq("PURCHASE_CONTRACT"),
                eq(800L),
                any(),
                eq("ERP_PURCHASE_CONTRACT"),
                eq("PC-001"),
                eq(LocalDate.of(2026, 8, 27)),
                eq(99L),
                any()
        );
    }

    @Test
    void dateRangeShouldRejectEndBeforeStart() {
        assertThrows(BizException.class, () -> service().create(new CreateRequest(
                "tenant-a", 1000L, 900L, "PC-004", LocalDate.of(2026, 8, 27),
                null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 1),
                null, null, null,
                List.of(new EntryRequest(301L, new BigDecimal("1"), null))
        ), 99L));
    }

    private PurchaseContractService service() {
        return new PurchaseContractService(
                contractMapper,
                contractEntryMapper,
                awardMapper,
                awardEntryMapper,
                rfqMapper,
                rfqEntryMapper,
                quoteEntryMapper,
                outboxService
        );
    }

    private SourcingAwardEntity award() {
        SourcingAwardEntity value = new SourcingAwardEntity();
        value.setFid(900L);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFrfqId(100L);
        value.setFnumber("AWD-001");
        value.setFstatus("CONFIRMED");
        return value;
    }

    private ProcurementRfqEntity rfq() {
        ProcurementRfqEntity value = new ProcurementRfqEntity();
        value.setFid(100L);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFnumber("RFQ-001");
        value.setFcurrencyCode("CNY");
        return value;
    }

    private SourcingAwardEntryEntity awardEntry(
            Long id, Long supplierId, String supplierCode, String supplierName
    ) {
        SourcingAwardEntryEntity value = new SourcingAwardEntryEntity();
        value.setFid(id);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFawardId(900L);
        value.setFrfqEntryId(id.equals(301L) ? 101L : 102L);
        value.setFquoteEntryId(id.equals(301L) ? 201L : 202L);
        value.setFbusinessPartnerId(supplierId);
        value.setFbusinessPartnerCode(supplierCode);
        value.setFbusinessPartnerName(supplierName);
        value.setFawardedQuantity(new BigDecimal("10"));
        value.setFunitPrice(new BigDecimal("100"));
        value.setFtaxRate(new BigDecimal("0.13"));
        return value;
    }

    private ProcurementRfqEntryEntity rfqEntry(Long id, Long requestEntryId) {
        ProcurementRfqEntryEntity value = new ProcurementRfqEntryEntity();
        value.setFid(id);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFpurchaseRequestId(1L);
        value.setFpurchaseRequestEntryId(requestEntryId);
        value.setFmaterialId(100L);
        value.setFmaterialCode("M001");
        value.setFmaterialName("物料1");
        value.setFunitId(1L);
        value.setFprojectId(300L);
        value.setFcostCenterId(400L);
        return value;
    }

    private SupplierQuoteEntryEntity quoteEntry() {
        SupplierQuoteEntryEntity value = new SupplierQuoteEntryEntity();
        value.setFid(201L);
        value.setFdeliveryDate(LocalDate.of(2026, 9, 15));
        return value;
    }

    private PurchaseContractEntity contract(String approval, String status) {
        PurchaseContractEntity value = new PurchaseContractEntity();
        value.setFid(800L);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFnumber("PC-001");
        value.setFdate(LocalDate.of(2026, 8, 27));
        value.setFsourcingAwardId(900L);
        value.setFbusinessPartnerId(500L);
        value.setFbusinessPartnerCode("SUP-A");
        value.setFbusinessPartnerName("供应商A");
        value.setFcurrencyCode("CNY");
        value.setFtotalQuantity(new BigDecimal("10"));
        value.setFnetAmount(new BigDecimal("1000.00"));
        value.setFtaxAmount(new BigDecimal("130.00"));
        value.setFgrossAmount(new BigDecimal("1130.00"));
        value.setFstatus(status);
        value.setFapprovalStatus(approval);
        value.setFexecutionStatus("NONE");
        value.setFversion(0);
        return value;
    }
}
