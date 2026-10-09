package single.cjj.erp.procurement.sourcing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntity;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntryEntity;
import single.cjj.erp.procurement.request.mapper.PurchaseRequestEntryMapper;
import single.cjj.erp.procurement.request.mapper.PurchaseRequestMapper;
import single.cjj.erp.procurement.sourcing.dto.SourcingContracts.*;
import single.cjj.erp.procurement.sourcing.entity.*;
import single.cjj.erp.procurement.sourcing.mapper.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcurementSourcingServiceTest {

    @Mock ProcurementRfqMapper rfqMapper;
    @Mock ProcurementRfqEntryMapper rfqEntryMapper;
    @Mock ProcurementRfqSupplierMapper rfqSupplierMapper;
    @Mock SupplierQuoteMapper quoteMapper;
    @Mock SupplierQuoteEntryMapper quoteEntryMapper;
    @Mock SourcingAwardMapper awardMapper;
    @Mock SourcingAwardEntryMapper awardEntryMapper;
    @Mock PurchaseRequestMapper requestMapper;
    @Mock PurchaseRequestEntryMapper requestEntryMapper;
    @Mock BusinessEventOutboxService outboxService;

    @Test
    void createRfqShouldRejectUnapprovedPurchaseRequest() {
        when(rfqMapper.selectCount(any())).thenReturn(0L);
        when(requestEntryMapper.selectByIdForUpdate(11L, "tenant-a")).thenReturn(requestEntry());
        when(requestMapper.selectByIdForUpdate(1L, "tenant-a"))
                .thenReturn(request("DRAFT", "DRAFT", "NONE"));

        assertThrows(BizException.class, () -> service().createRfq(rfqCreate(), 99L));
    }

    @Test
    void publishShouldMoveRequestIntoSourcing() {
        ProcurementRfqEntity rfq = rfq("DRAFT");
        ProcurementRfqEntryEntity rfqEntry = rfqEntry();
        ProcurementRfqSupplierEntity supplier = supplier();
        PurchaseRequestEntity request = request("APPROVED", "EFFECTIVE", "NONE");

        when(rfqMapper.selectByIdForUpdate(100L, "tenant-a")).thenReturn(rfq);
        when(rfqEntryMapper.selectList(any())).thenReturn(List.of(rfqEntry));
        when(rfqSupplierMapper.selectList(any())).thenReturn(List.of(supplier));
        when(requestMapper.selectByIdForUpdate(1L, "tenant-a")).thenReturn(request);
        when(requestMapper.updateById(any())).thenReturn(1);
        when(rfqMapper.updateById(any())).thenReturn(1);

        RfqDetail detail = service().publishRfq(100L, "tenant-a", 99L);

        assertEquals("SOURCING", request.getFexecutionStatus());
        assertEquals("PUBLISHED", detail.rfq().getFstatus());
    }

    @Test
    void quoteShouldCalculateTaxAndGrossAmount() {
        ProcurementRfqEntity rfq = rfq("PUBLISHED");
        when(rfqMapper.selectByIdForUpdate(100L, "tenant-a")).thenReturn(rfq);
        when(rfqSupplierMapper.selectOne(any())).thenReturn(supplier());
        when(quoteMapper.selectCount(any())).thenReturn(0L);
        when(rfqEntryMapper.selectOne(any())).thenReturn(rfqEntry());
        when(quoteMapper.insert(any())).thenReturn(1);
        when(quoteEntryMapper.insert(any())).thenReturn(1);

        QuoteDetail detail = service().createQuote(100L, new QuoteCreateRequest(
                "tenant-a", 500L, "Q-001", LocalDate.of(2026, 8, 27),
                LocalDate.of(2026, 9, 30), 7, "月结30天", null,
                List.of(new QuoteEntryRequest(
                        101L, new BigDecimal("10"), new BigDecimal("100"),
                        new BigDecimal("0.13"), LocalDate.of(2026, 9, 15), null))
        ), 99L);

        assertEquals(new BigDecimal("1000.00"), detail.quote().getFnetAmount());
        assertEquals(new BigDecimal("130.00"), detail.quote().getFtaxAmount());
        assertEquals(new BigDecimal("1130.00"), detail.quote().getFgrossAmount());
    }

    @Test
    void comparisonShouldMarkLowestGrossUnitPriceWithoutForcingWinner() {
        ProcurementRfqEntity rfq = rfq("PUBLISHED");
        SupplierQuoteEntity q1 = quote(201L, 500L, "SUP-A", "供应商A");
        SupplierQuoteEntity q2 = quote(202L, 501L, "SUP-B", "供应商B");
        SupplierQuoteEntryEntity e1 = quoteEntry(301L, 201L, new BigDecimal("100"), new BigDecimal("0.13"));
        SupplierQuoteEntryEntity e2 = quoteEntry(302L, 202L, new BigDecimal("105"), BigDecimal.ZERO);
        e1.setFgrossAmount(new BigDecimal("1130.00"));
        e2.setFgrossAmount(new BigDecimal("1050.00"));

        when(rfqMapper.selectOne(any())).thenReturn(rfq);
        when(quoteMapper.selectList(any())).thenReturn(List.of(q1, q2));
        when(quoteEntryMapper.selectList(any())).thenReturn(List.of(e2, e1));

        List<ComparisonLine> lines = service().comparison(100L, "tenant-a");

        assertEquals(2, lines.size());
        ComparisonLine cheapest = lines.stream().filter(ComparisonLine::lowestGrossUnitPrice).findFirst().orElseThrow();
        assertEquals(501L, cheapest.businessPartnerId());
        assertEquals(new BigDecimal("105.000000"), cheapest.grossUnitPrice());
    }

    @Test
    void awardShouldWriteBackSourcedQuantityCloseRfqAndEmitEvent() {
        ProcurementRfqEntity rfq = rfq("PUBLISHED");
        ProcurementRfqEntryEntity rfqEntry = rfqEntry();
        SupplierQuoteEntity quote = quote(201L, 500L, "SUP-001", "供应商A");
        SupplierQuoteEntryEntity quoteEntry = quoteEntry(301L, 201L, new BigDecimal("100"), new BigDecimal("0.13"));
        PurchaseRequestEntryEntity requestEntry = requestEntry();
        PurchaseRequestEntity request = request("APPROVED", "EFFECTIVE", "SOURCING");

        when(rfqMapper.selectByIdForUpdate(100L, "tenant-a")).thenReturn(rfq);
        when(awardMapper.selectCount(any())).thenReturn(0L);
        when(rfqEntryMapper.selectByIdForUpdate(101L, "tenant-a")).thenReturn(rfqEntry);
        when(quoteMapper.selectByIdForUpdate(201L, "tenant-a")).thenReturn(quote);
        when(quoteEntryMapper.selectByIdForUpdate(301L, "tenant-a")).thenReturn(quoteEntry);
        when(requestEntryMapper.selectByIdForUpdate(11L, "tenant-a")).thenReturn(requestEntry);
        when(rfqEntryMapper.updateById(any())).thenReturn(1);
        when(quoteEntryMapper.updateById(any())).thenReturn(1);
        when(requestEntryMapper.updateById(any())).thenReturn(1);
        when(awardMapper.insert(any())).thenReturn(1);
        when(awardEntryMapper.insert(any())).thenReturn(1);
        when(requestMapper.selectByIdForUpdate(1L, "tenant-a")).thenReturn(request);
        when(requestEntryMapper.selectList(any())).thenReturn(List.of(requestEntry));
        when(requestMapper.updateById(any())).thenReturn(1);
        when(rfqEntryMapper.selectList(any())).thenReturn(List.of(rfqEntry));
        when(rfqMapper.updateById(any())).thenReturn(1);
        when(rfqEntryMapper.selectById(anyLong())).thenReturn(rfqEntry);

        AwardDetail detail = service().confirmAward(100L, new AwardCreateRequest(
                "tenant-a", "AWD-001", LocalDate.of(2026, 8, 27), "综合定标",
                List.of(new AwardEntryRequest(101L, 201L, 301L, new BigDecimal("10"), "满足价格和交期"))
        ), 99L);

        assertEquals(new BigDecimal("10"), requestEntry.getFsourcedQuantity());
        assertEquals("CONTRACTING", request.getFexecutionStatus());
        assertEquals("CLOSED", rfq.getFstatus());
        assertEquals(new BigDecimal("1130.00"), detail.award().getFgrossAmount());
        verify(outboxService).append(
                eq("tenant-a"), eq(1000L), eq("PURCHASE_SOURCING_AWARDED"),
                eq("SOURCING_AWARD"), anyLong(), anyLong(),
                eq("ERP_SOURCING_AWARD"), eq("AWD-001"),
                eq(LocalDate.of(2026, 8, 27)), eq(99L), any());
    }

    @Test
    void awardShouldRejectQuantityBeyondRemainingRequestQuantity() {
        ProcurementRfqEntity rfq = rfq("PUBLISHED");
        ProcurementRfqEntryEntity rfqEntry = rfqEntry();
        rfqEntry.setFquantity(new BigDecimal("20"));
        SupplierQuoteEntryEntity quoteEntry = quoteEntry(301L, 201L, new BigDecimal("100"), BigDecimal.ZERO);
        quoteEntry.setFquantity(new BigDecimal("20"));
        PurchaseRequestEntryEntity requestEntry = requestEntry();
        requestEntry.setFsourcedQuantity(new BigDecimal("5"));

        when(rfqMapper.selectByIdForUpdate(100L, "tenant-a")).thenReturn(rfq);
        when(awardMapper.selectCount(any())).thenReturn(0L);
        when(rfqEntryMapper.selectByIdForUpdate(101L, "tenant-a")).thenReturn(rfqEntry);
        when(quoteMapper.selectByIdForUpdate(201L, "tenant-a"))
                .thenReturn(quote(201L, 500L, "SUP-001", "供应商A"));
        when(quoteEntryMapper.selectByIdForUpdate(301L, "tenant-a")).thenReturn(quoteEntry);
        when(requestEntryMapper.selectByIdForUpdate(11L, "tenant-a")).thenReturn(requestEntry);

        assertThrows(BizException.class, () -> service().confirmAward(100L, new AwardCreateRequest(
                "tenant-a", "AWD-002", LocalDate.of(2026, 8, 27), null,
                List.of(new AwardEntryRequest(101L, 201L, 301L, new BigDecimal("10"), null))
        ), 99L));
    }

    private ProcurementSourcingService service() {
        return new ProcurementSourcingService(
                rfqMapper, rfqEntryMapper, rfqSupplierMapper, quoteMapper, quoteEntryMapper,
                awardMapper, awardEntryMapper, requestMapper, requestEntryMapper, outboxService);
    }

    private RfqCreateRequest rfqCreate() {
        return new RfqCreateRequest(
                "tenant-a", 1000L, "RFQ-001", LocalDate.of(2026, 8, 27),
                "项目物料询价", "CNY", null, null,
                List.of(new RfqEntryRequest(1L, 11L, new BigDecimal("10"))),
                List.of(new RfqSupplierRequest(500L, "SUP-001", "供应商A")));
    }

    private ProcurementRfqEntity rfq(String status) {
        ProcurementRfqEntity value = new ProcurementRfqEntity();
        value.setFid(100L); value.setFtenantId("tenant-a"); value.setForgId(1000L);
        value.setFnumber("RFQ-001"); value.setFdate(LocalDate.of(2026, 8, 27));
        value.setFcurrencyCode("CNY"); value.setFstatus(status); value.setFversion(0);
        return value;
    }

    private ProcurementRfqEntryEntity rfqEntry() {
        ProcurementRfqEntryEntity value = new ProcurementRfqEntryEntity();
        value.setFid(101L); value.setFtenantId("tenant-a"); value.setForgId(1000L); value.setFrfqId(100L);
        value.setFpurchaseRequestId(1L); value.setFpurchaseRequestEntryId(11L); value.setFmaterialId(1L);
        value.setFmaterialCode("M001"); value.setFmaterialName("物料1"); value.setFquantity(new BigDecimal("10"));
        value.setFawardedQuantity(BigDecimal.ZERO); value.setFversion(0);
        return value;
    }

    private ProcurementRfqSupplierEntity supplier() {
        ProcurementRfqSupplierEntity value = new ProcurementRfqSupplierEntity();
        value.setFid(150L); value.setFtenantId("tenant-a"); value.setForgId(1000L); value.setFrfqId(100L);
        value.setFbusinessPartnerId(500L); value.setFbusinessPartnerCode("SUP-001");
        value.setFbusinessPartnerName("供应商A"); value.setFstatus("INVITED");
        return value;
    }

    private SupplierQuoteEntity quote(Long id, Long supplierId, String code, String name) {
        SupplierQuoteEntity value = new SupplierQuoteEntity();
        value.setFid(id); value.setFtenantId("tenant-a"); value.setForgId(1000L); value.setFrfqId(100L);
        value.setFbusinessPartnerId(supplierId); value.setFbusinessPartnerCode(code); value.setFbusinessPartnerName(name);
        value.setFquoteNo("Q-" + id); value.setFstatus("SUBMITTED");
        return value;
    }

    private SupplierQuoteEntryEntity quoteEntry(Long id, Long quoteId, BigDecimal price, BigDecimal taxRate) {
        SupplierQuoteEntryEntity value = new SupplierQuoteEntryEntity();
        value.setFid(id); value.setFtenantId("tenant-a"); value.setForgId(1000L); value.setFquoteId(quoteId);
        value.setFrfqEntryId(101L); value.setFquantity(new BigDecimal("10")); value.setFawardedQuantity(BigDecimal.ZERO);
        value.setFunitPrice(price); value.setFtaxRate(taxRate); value.setFgrossAmount(new BigDecimal("1130.00"));
        return value;
    }

    private PurchaseRequestEntity request(String approval, String status, String execution) {
        PurchaseRequestEntity value = new PurchaseRequestEntity();
        value.setFid(1L); value.setFtenantId("tenant-a"); value.setForgId(1000L); value.setFnumber("PR-001");
        value.setFdate(LocalDate.of(2026, 8, 27)); value.setFcurrencyCode("CNY");
        value.setFapprovalStatus(approval); value.setFstatus(status); value.setFexecutionStatus(execution); value.setFversion(0);
        return value;
    }

    private PurchaseRequestEntryEntity requestEntry() {
        PurchaseRequestEntryEntity value = new PurchaseRequestEntryEntity();
        value.setFid(11L); value.setFtenantId("tenant-a"); value.setForgId(1000L); value.setFpurchaseRequestId(1L);
        value.setFlineNo(1); value.setFmaterialId(1L); value.setFmaterialCode("M001"); value.setFmaterialName("物料1");
        value.setFquantity(new BigDecimal("10")); value.setFsourcedQuantity(BigDecimal.ZERO);
        value.setForderedQuantity(BigDecimal.ZERO); value.setFversion(0);
        return value;
    }
}
