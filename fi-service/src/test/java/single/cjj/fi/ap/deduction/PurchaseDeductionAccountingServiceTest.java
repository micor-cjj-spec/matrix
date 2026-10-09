package single.cjj.fi.ap.deduction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.accounting.model.AccountingModels.*;
import single.cjj.fi.accounting.persistence.InboundAccountingRepository;
import single.cjj.fi.accounting.service.AccountingRuleEngine;
import single.cjj.fi.ap.deduction.PurchaseDeductionAccountingRepository.PayableCandidate;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.service.BizfiFiVoucherService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PurchaseDeductionAccountingServiceTest {

    @Test
    void shouldDeductOnlyUnreservedFormalApAndCreateVoucher() {
        InboundAccountingRepository common = mock(InboundAccountingRepository.class);
        PurchaseDeductionAccountingRepository repository = mock(PurchaseDeductionAccountingRepository.class);
        AccountingRuleEngine engine = mock(AccountingRuleEngine.class);
        BizfiFiVoucherService voucherService = mock(BizfiFiVoucherService.class);
        BizfiFiVoucherMapper voucherMapper = mock(BizfiFiVoucherMapper.class);

        PurchaseDeductionAccountingService service = new PurchaseDeductionAccountingService(
                new ObjectMapper(), common, repository, engine,
                voucherService, voucherMapper,
                "FI_PURCHASE_DEDUCTION_ACCOUNTING_V1", "DEFAULT");

        when(common.findInboxStatus("FI_PURCHASE_DEDUCTION_ACCOUNTING_V1","BE-DED-1")).thenReturn(null);
        when(common.insertInbox(anyLong(),eq("FI_PURCHASE_DEDUCTION_ACCOUNTING_V1"),any())).thenReturn(1);
        when(repository.findFormalPayableCandidatesForUpdate(
                "T1",1L,"BP-1","CNY","101"))
                .thenReturn(List.of(new PayableCandidate(
                        501L,"AP-001", LocalDate.of(2026,8,20),
                        new BigDecimal("500.00"),new BigDecimal("100.00"),
                        "OPEN",601L,new BigDecimal("300.00"),BigDecimal.ZERO.setScale(2)
                )));

        RuleHeader rule = new RuleHeader(
                1L,2L,"P1_PURCHASE_DEDUCTION_RECOGNITION",
                1,100,1,"FORMAL","DEFAULT");
        RuleEvaluation evaluation = new RuleEvaluation(
                rule,
                List.of(
                        new AccountingLine(10,11L,null,"DEBIT","FORMAL_AP","2202",
                                "采购扣款-DED-001",new BigDecimal("100.00"),BigDecimal.ZERO.setScale(2),
                                "CNY",new BigDecimal("100.00"),List.of()),
                        new AccountingLine(20,12L,null,"CREDIT","PURCHASE_CLAIM_RECOVERY","6051",
                                "采购扣款-DED-001",BigDecimal.ZERO.setScale(2),new BigDecimal("100.00"),
                                "CNY",new BigDecimal("100.00"),List.of())
                ),
                new BigDecimal("100.00"),new BigDecimal("100.00"));
        when(engine.evaluate(eq("PURCHASE_DEDUCTION_RECOGNITION"),any(EventContext.class)))
                .thenReturn(evaluation);

        when(voucherMapper.selectOne(any())).thenReturn(null);
        BizfiFiVoucher voucher=new BizfiFiVoucher();
        voucher.setFid(900L);voucher.setFnumber("V-DED-001");voucher.setTenantId("T1");
        doAnswer(invocation -> {
            BizfiFiVoucher draft=invocation.getArgument(0);
            draft.setFid(900L);draft.setFnumber("V-DED-001");return draft;
        }).when(voucherService).saveDraft(any(BizfiFiVoucher.class));
        when(voucherService.saveLines(eq(900L),any())).thenReturn(true);
        when(voucherService.get(900L)).thenReturn(voucher);
        BizfiFiVoucherLine l1=new BizfiFiVoucherLine();l1.setFid(901L);l1.setFlineNo(10);
        BizfiFiVoucherLine l2=new BizfiFiVoucherLine();l2.setFid(902L);l2.setFlineNo(20);
        when(voucherService.listLines(900L)).thenReturn(List.of(l1,l2));

        ProcessingResult result=service.process(eventJson("100.00"));

        assertFalse(result.duplicate());
        assertEquals(501L,result.payableId());
        assertEquals(900L,result.voucherId());
        verify(repository).applyPayableDeduction(
                501L,"T1",new BigDecimal("100.00"),new BigDecimal("400.00"),
                "PARTIAL_SETTLED",99L);
        verify(repository).insertAllocation(
                anyLong(),eq("T1"),eq(1L),anyLong(),eq("BE-DED-1"),
                eq("801"),eq("101"),eq(501L),eq(601L),
                eq(new BigDecimal("100.00")),eq(new BigDecimal("500.00")),
                eq(new BigDecimal("400.00")),eq(99L));
        verify(engine).evaluate(eq("PURCHASE_DEDUCTION_RECOGNITION"),any(EventContext.class));
        verify(common).markInboxProcessed(
                eq("FI_PURCHASE_DEDUCTION_ACCOUNTING_V1"),eq("BE-DED-1"),
                eq(501L),any(),eq(900L),eq("V-DED-001"));
    }

    @Test
    void shouldRejectWhenOnlyReservedApBalanceRemains() {
        InboundAccountingRepository common = mock(InboundAccountingRepository.class);
        PurchaseDeductionAccountingRepository repository = mock(PurchaseDeductionAccountingRepository.class);
        AccountingRuleEngine engine = mock(AccountingRuleEngine.class);
        BizfiFiVoucherService voucherService = mock(BizfiFiVoucherService.class);
        BizfiFiVoucherMapper voucherMapper = mock(BizfiFiVoucherMapper.class);

        PurchaseDeductionAccountingService service = new PurchaseDeductionAccountingService(
                new ObjectMapper(), common, repository, engine,
                voucherService, voucherMapper,
                "FI_PURCHASE_DEDUCTION_ACCOUNTING_V1", "DEFAULT");

        when(common.findInboxStatus("FI_PURCHASE_DEDUCTION_ACCOUNTING_V1","BE-DED-1")).thenReturn(null);
        when(common.insertInbox(anyLong(),eq("FI_PURCHASE_DEDUCTION_ACCOUNTING_V1"),any())).thenReturn(1);
        when(repository.findFormalPayableCandidatesForUpdate(
                "T1",1L,"BP-1","CNY","101"))
                .thenReturn(List.of(new PayableCandidate(
                        501L,"AP-001",LocalDate.of(2026,8,20),
                        new BigDecimal("100.00"),new BigDecimal("80.00"),
                        "OPEN",601L,new BigDecimal("100.00"),BigDecimal.ZERO.setScale(2)
                )));

        assertThrows(BizException.class,()->service.process(eventJson("50.00")));
        verify(engine,never()).evaluate(anyString(),any());
    }

    private String eventJson(String amount) {
        return """
                {
                  "eventId":"BE-DED-1",
                  "eventType":"PURCHASE_DEDUCTION_CONFIRMED",
                  "eventVersion":1,
                  "tenantId":"T1",
                  "orgId":1,
                  "producerService":"erp-service",
                  "domainCode":"PROCUREMENT",
                  "aggregateType":"PURCHASE_DEDUCTION",
                  "aggregateId":"800",
                  "aggregateVersion":1,
                  "sourceSystemCode":"MATRIX",
                  "sourceDocumentType":"ERP_PURCHASE_DEDUCTION",
                  "sourceDocumentId":"800",
                  "sourceDocumentNo":"DED-001",
                  "businessDate":"2026-08-27",
                  "operatorId":99,
                  "payload":{
                    "purchaseDeductionId":800,
                    "purchaseDeductionNo":"DED-001",
                    "supplierClaimId":700,
                    "purchaseOrderId":100,
                    "businessPartnerId":"BP-1",
                    "businessPartnerCode":"SUP-1",
                    "businessPartnerName":"供应商1",
                    "currencyCode":"CNY",
                    "amount":%s,
                    "entries":[
                      {
                        "purchaseDeductionEntryId":801,
                        "supplierClaimEntryId":701,
                        "purchaseOrderId":100,
                        "purchaseOrderEntryId":101,
                        "materialId":1,
                        "materialCode":"M001",
                        "amount":%s
                      }
                    ]
                  }
                }
                """.formatted(amount,amount);
    }
}
