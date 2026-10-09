package single.cjj.erp.sales.commercial.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.crm.opportunity.entity.CrmOpportunityEntity;
import single.cjj.erp.crm.opportunity.mapper.CrmOpportunityMapper;
import single.cjj.erp.crm.support.CustomerPartnerValidator;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.integration.base.BaseBusinessPartnerContracts.BusinessPartnerDetail;
import single.cjj.erp.sales.commercial.dto.SalesCommercialContracts.*;
import single.cjj.erp.sales.commercial.entity.*;
import single.cjj.erp.sales.commercial.mapper.*;
import single.cjj.erp.sales.commercial.workflow.SalesWorkflowCoordinator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesCommercialServiceTest {
    @Mock SalesQuoteMapper quotes;
    @Mock SalesQuoteEntryMapper quoteEntries;
    @Mock SalesContractMapper contracts;
    @Mock SalesContractEntryMapper contractEntries;
    @Mock CrmOpportunityMapper opportunities;
    @Mock CustomerPartnerValidator customers;
    @Mock BusinessEventOutboxService outbox;
    @Mock SalesCommercialActionAuditMapper audits;
    @Mock SalesWorkflowCoordinator workflow;

    private SalesCommercialService service() {
        lenient().when(audits.insert(any())).thenReturn(1);
        lenient().when(workflow.enabled()).thenReturn(true);
        return new SalesCommercialService(quotes, quoteEntries, contracts,
                contractEntries, opportunities, customers, outbox, audits, workflow);
    }

    private SalesQuoteEntity quote(String status) {
        SalesQuoteEntity q = new SalesQuoteEntity();
        q.setFid(10L); q.setFtenantId("T1"); q.setForgId(3L);
        q.setFnumber("SQ-10"); q.setFdate(LocalDate.now());
        q.setFopportunityId(20L); q.setFbusinessPartnerId(30L);
        q.setFstatus(status); q.setFvalidUntil(LocalDate.now().plusDays(10));
        q.setFcurrencyCode("CNY"); q.setFnetAmount(new BigDecimal("100.00"));
        q.setFtaxAmount(new BigDecimal("13.00")); q.setFgrossAmount(new BigDecimal("113.00"));
        q.setFversion(0); q.setFcreateBy(9L); return q;
    }

    private CrmOpportunityEntity opportunity() {
        CrmOpportunityEntity o = new CrmOpportunityEntity();
        o.setFid(20L); o.setFtenantId("T1"); o.setForgId(3L); o.setFbusinessPartnerId(30L);
        o.setFstatus("OPEN"); return o;
    }

    private CreateQuote request(Long partnerId, String type, String tenderRef) {
        return new CreateQuote("T1", 3L, null, 20L, partnerId,
                "CNY", type, tenderRef, LocalDate.now().plusDays(10), "DDP", "NET30",
                List.of(new QuoteLine("服务", "SVC1", new BigDecimal("2"),
                        new BigDecimal("50.00"), new BigDecimal("13"))));
    }

    private void customerGate() {
        when(opportunities.selectOne(any(Wrapper.class))).thenReturn(opportunity());
        when(customers.requireActiveCustomer(30L, "T1")).thenReturn(
                new BusinessPartnerDetail(30L, "T1", "C001", "客户A",
                        "ORGANIZATION", null, "ACTIVE", "AUDITED", List.of("CUSTOMER")));
    }

    @Test
    void quoteShouldRecomputeAmountsAndSnapshotCustomer() {
        customerGate();
        when(quotes.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(quotes.insert(any())).thenReturn(1);
        when(quoteEntries.insert(any())).thenReturn(1);
        QuoteDetail detail = service().createQuote(request(30L, "QUOTE", null), 7L);
        assertEquals(new BigDecimal("100.00"), detail.header().getFnetAmount());
        assertEquals(new BigDecimal("13.00"), detail.header().getFtaxAmount());
        assertEquals(new BigDecimal("113.00"), detail.header().getFgrossAmount());
        assertEquals("C001", detail.header().getFbusinessPartnerCode());
        assertEquals(1, detail.entries().size());
        assertEquals("DRAFT", detail.header().getFstatus());
        verifyNoInteractions(outbox);
    }

    @Test
    void mismatchedOpportunityCustomerMustBeRejected() {
        when(opportunities.selectOne(any(Wrapper.class))).thenReturn(opportunity());
        assertThrows(BizException.class,
                () -> service().createQuote(request(31L, "QUOTE", null), 7L));
        verify(quotes, never()).insert(any());
        verifyNoInteractions(customers);
    }

    @Test
    void tenderQuoteRequiresTenderReference() {
        customerGate();
        assertThrows(BizException.class,
                () -> service().createQuote(request(30L, "TENDER", null), 7L));
        verify(quotes, never()).insert(any());
    }

    @Test
    void onlySentUnexpiredQuoteCanBeAcceptedAndPublished() {
        when(quotes.selectByIdForUpdate(10L, "T1")).thenReturn(quote("SENT"));
        when(quotes.updateById(any())).thenReturn(1);
        SalesQuoteEntity result = service().transitionQuote(10L, "T1", "accept", 7L);
        assertEquals("ACCEPTED", result.getFstatus());
        assertNotNull(result.getFacceptedTime());
        verify(outbox).append(eq("T1"), eq(3L), eq("SALES"), eq("SALES_QUOTE_ACCEPTED"),
                eq("SALES_QUOTE"), eq(10L), anyLong(),
                eq("ERP_SALES_QUOTE"), eq("SQ-10"), any(LocalDate.class), eq(7L), any());
    }

    @Test
    void draftOrExpiredQuoteCannotBeAccepted() {
        SalesQuoteEntity q = quote("DRAFT");
        when(quotes.selectByIdForUpdate(10L, "T1")).thenReturn(q);
        assertThrows(BizException.class,
                () -> service().transitionQuote(10L, "T1", "accept", 7L));
        q.setFstatus("SENT"); q.setFvalidUntil(LocalDate.now().minusDays(1));
        assertThrows(BizException.class,
                () -> service().transitionQuote(10L, "T1", "accept", 7L));
        verify(quotes, never()).updateById(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void contractMustHaveAcceptedQuote() {
        when(quotes.selectByIdForUpdate(10L, "T1")).thenReturn(quote("SENT"));
        CreateContract request = new CreateContract("T1", null, 10L, "年度服务合同",
                LocalDate.now(), LocalDate.now().plusMonths(6));
        assertThrows(BizException.class, () -> service().createContract(request, 7L));
        verify(contracts, never()).insert(any());
    }

    @Test
    void updateDraftQuoteRecalculatesTotalsAndReplacesLinesWithinTenant() {
        SalesQuoteEntity q = quote("DRAFT");
        when(quotes.selectByIdForUpdate(10L, "T1")).thenReturn(q);
        when(quotes.updateById(q)).thenReturn(1);
        when(quoteEntries.insert(any())).thenReturn(1);
        UpdateQuote request = new UpdateQuote("T1", LocalDate.now().plusDays(14),
                "EXW", "NET45", List.of(
                        new QuoteLine("新服务", "SV2", new BigDecimal("3"),
                                new BigDecimal("60"), new BigDecimal("6"))));
        QuoteDetail result = service().updateQuote(10L, request, 7L);
        assertEquals(new BigDecimal("180.00"), result.header().getFnetAmount());
        assertEquals(new BigDecimal("10.80"), result.header().getFtaxAmount());
        assertEquals(new BigDecimal("190.80"), result.header().getFgrossAmount());
        assertEquals("NET45", result.header().getFpaymentTermCode());
        verify(quoteEntries).deleteDraftEntries(10L, "T1");
        verify(quoteEntries).insert(any());
    }

    @Test
    void approvedQuoteCannotBeEditedOrCancelled() {
        when(quotes.selectByIdForUpdate(10L, "T1")).thenReturn(quote("APPROVED"));
        UpdateQuote request = new UpdateQuote("T1", LocalDate.now().plusDays(14),
                null, null, List.of(new QuoteLine("服务", null, BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.ZERO)));
        assertThrows(BizException.class, () -> service().updateQuote(10L, request, 7L));
        assertThrows(BizException.class, () -> service().transitionQuote(10L, "T1", "cancel", 7L));
        verify(quotes, never()).updateById(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void submittedQuoteCanBeWithdrawnButNotAccepted() {
        SalesQuoteEntity q = quote("SUBMITTED");
        when(quotes.selectByIdForUpdate(10L, "T1")).thenReturn(q);
        when(quotes.updateById(q)).thenReturn(1);
        SalesCommercialService target=service();
        when(workflow.enabled()).thenReturn(false);
        assertThrows(BizException.class, () -> target.transitionQuote(10L, "T1", "accept", 7L));
        assertEquals("DRAFT", target.transitionQuote(10L, "T1", "withdraw", 7L).getFstatus());
        verifyNoInteractions(outbox);
    }

    @Test
    void draftCancellationAndSentExpirationPublishSeparateEvents() {
        SalesQuoteEntity q = quote("DRAFT");
        when(quotes.selectByIdForUpdate(10L, "T1")).thenReturn(q);
        when(quotes.updateById(q)).thenReturn(1);
        assertEquals("CANCELLED", service().transitionQuote(10L, "T1", "cancel", 7L).getFstatus());
        verify(outbox).append(eq("T1"), eq(3L), eq("SALES"), eq("SALES_QUOTE_CANCELLED"),
                eq("SALES_QUOTE"), eq(10L), anyLong(), eq("ERP_SALES_QUOTE"),
                eq("SQ-10"), any(LocalDate.class), eq(7L), any());
        q.setFstatus("SENT"); q.setFvalidUntil(LocalDate.now().minusDays(1));
        assertEquals("EXPIRED", service().transitionQuote(10L, "T1", "expire", 7L).getFstatus());
        verify(outbox).append(eq("T1"), eq(3L), eq("SALES"), eq("SALES_QUOTE_EXPIRED"),
                eq("SALES_QUOTE"), eq(10L), anyLong(), eq("ERP_SALES_QUOTE"),
                eq("SQ-10"), any(LocalDate.class), eq(7L), any());
    }

    @Test
    void quoteApprovalRejectsTheOriginalCreator() {
        SalesQuoteEntity q = quote("SUBMITTED");
        q.setFcreateBy(7L);
        when(quotes.selectByIdForUpdate(10L, "T1")).thenReturn(q);
        assertThrows(BizException.class,
                () -> service().transitionQuoteFromWorkflow(10L, "T1", true, 7L));
        verify(quotes, never()).updateById(any());
        verify(audits, never()).insert(any());
    }

    @Test
    void contractApprovalRejectsTheOriginalCreator() {
        SalesContractEntity c = new SalesContractEntity();
        c.setFid(100L); c.setFtenantId("T1"); c.setForgId(3L);
        c.setFstatus("DRAFT"); c.setFapprovalStatus("SUBMITTED");
        c.setFcreateBy(7L);
        when(contracts.selectByIdForUpdate(100L, "T1")).thenReturn(c);
        assertThrows(BizException.class,
                () -> service().transitionContractFromWorkflow(100L, "T1", true, 7L));
        verify(contracts, never()).updateById(any());
        verify(audits, never()).insert(any());
    }

    @Test
    void acceptedQuoteProducesAuditWithExactStateChangeAndActor() {
        when(quotes.selectByIdForUpdate(10L, "T1")).thenReturn(quote("SENT"));
        when(quotes.updateById(any())).thenReturn(1);
        service().transitionQuote(10L, "T1", "accept", 7L);
        verify(audits).insert(argThat(entry ->
                "T1".equals(entry.getFtenantId()) && Long.valueOf(3L).equals(entry.getForgId())
                        && "SALES_QUOTE".equals(entry.getFdocumentType())
                        && "ACCEPT".equals(entry.getFaction())
                        && "SENT".equals(entry.getFbeforeStatus())
                        && "ACCEPTED".equals(entry.getFafterStatus())
                        && Long.valueOf(7L).equals(entry.getFoperatorId())));
    }

    @Test
    void auditFailureMustPreventSuccessfulTransition() {
        SalesQuoteEntity q = quote("SUBMITTED");
        when(quotes.selectByIdForUpdate(10L, "T1")).thenReturn(q);
        when(quotes.updateById(any())).thenReturn(1);
        SalesCommercialService target = service();
        when(audits.insert(any())).thenReturn(0);
        assertThrows(BizException.class,
                () -> target.transitionQuoteFromWorkflow(10L, "T1", true, 8L));
    }

    @Test
    void contractApprovalPublishesOnlyBusinessEvent() {
        SalesContractEntity c = new SalesContractEntity();
        c.setFid(100L); c.setFtenantId("T1"); c.setForgId(3L);
        c.setFnumber("SC-100"); c.setFdate(LocalDate.now());
        c.setFquoteId(10L); c.setFbusinessPartnerId(30L);
        c.setFgrossAmount(new BigDecimal("113.00")); c.setFversion(0);
        c.setFapprovalStatus("SUBMITTED"); c.setFstatus("DRAFT"); c.setFcreateBy(9L);
        when(contracts.selectByIdForUpdate(100L, "T1")).thenReturn(c);
        when(contracts.updateById(c)).thenReturn(1);
        SalesContractEntity result = service().transitionContractFromWorkflow(100L, "T1", true, 7L);
        assertEquals("EFFECTIVE", result.getFstatus());
        assertEquals("APPROVED", result.getFapprovalStatus());
        verify(outbox).append(eq("T1"), eq(3L), eq("SALES"), eq("SALES_CONTRACT_EFFECTIVE"),
                eq("SALES_CONTRACT"), eq(100L), anyLong(),
                eq("ERP_SALES_CONTRACT"), eq("SC-100"), any(LocalDate.class), eq(7L), any());
    }
}
