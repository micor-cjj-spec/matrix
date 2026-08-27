package single.cjj.fi.accounting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import single.cjj.fi.accounting.model.AccountingModels.AccountingLine;
import single.cjj.fi.accounting.model.AccountingModels.EventContext;
import single.cjj.fi.accounting.model.AccountingModels.ProcessingResult;
import single.cjj.fi.accounting.model.AccountingModels.RuleEvaluation;
import single.cjj.fi.accounting.model.AccountingModels.RuleHeader;
import single.cjj.fi.accounting.persistence.InboundAccountingRepository;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.SettlementRow;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.service.BizfiFiVoucherService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentCompletedAccountingServiceTest {

    @Test
    void shouldCreatePaymentAccountingEventVoucherAndTrace() {
        InboundAccountingRepository commonRepository =
                mock(InboundAccountingRepository.class);
        PaymentSettlementRepository settlementRepository =
                mock(PaymentSettlementRepository.class);
        AccountingRuleEngine ruleEngine = mock(AccountingRuleEngine.class);
        BizfiFiVoucherService voucherService =
                mock(BizfiFiVoucherService.class);
        BizfiFiVoucherMapper voucherMapper =
                mock(BizfiFiVoucherMapper.class);

        PaymentCompletedAccountingService service =
                new PaymentCompletedAccountingService(
                        new ObjectMapper(),
                        commonRepository,
                        settlementRepository,
                        ruleEngine,
                        voucherService,
                        voucherMapper,
                        "FI_PAYMENT_COMPLETED_ACCOUNTING_V1",
                        "DEFAULT"
                );

        String rawJson = eventJson("BE-PAY-001");
        when(commonRepository.findInboxStatus(
                "FI_PAYMENT_COMPLETED_ACCOUNTING_V1", "BE-PAY-001"))
                .thenReturn(null);
        when(commonRepository.insertInbox(
                anyLong(),
                eq("FI_PAYMENT_COMPLETED_ACCOUNTING_V1"),
                any()
        )).thenReturn(1);

        SettlementRow settlement = new SettlementRow(
                80L, "T1", 1L, "APSET-001", 40L, 50L,
                "BP-1", "SUP-1", "供应商1", "CNY",
                new BigDecimal("600"), "COMPLETED",
                LocalDate.of(2026, 8, 27), "BE-PAY-001",
                null, null, null, 99L,
                LocalDateTime.of(2026, 8, 27, 10, 31), 1
        );
        when(settlementRepository.findSettlement(80L, "T1"))
                .thenReturn(settlement);

        RuleHeader rule = new RuleHeader(
                1L, 11L, "P0_PURCHASE_PAYMENT_RECOGNITION",
                1, 100, 1, "FORMAL", "DEFAULT");
        List<AccountingLine> lines = List.of(
                new AccountingLine(
                        1, 101L, null, "DEBIT",
                        "FORMAL_AP", "2202", "采购付款-PAYORD-001",
                        new BigDecimal("600.00"), BigDecimal.ZERO.setScale(2),
                        "CNY", new BigDecimal("600.00"), List.of()),
                new AccountingLine(
                        2, 102L, null, "CREDIT",
                        "BANK_DEPOSIT", "1002", "采购付款-PAYORD-001",
                        BigDecimal.ZERO.setScale(2), new BigDecimal("600.00"),
                        "CNY", new BigDecimal("600.00"), List.of())
        );
        RuleEvaluation evaluation = new RuleEvaluation(
                rule, lines, new BigDecimal("600.00"), new BigDecimal("600.00"));
        when(ruleEngine.evaluate(
                eq("PURCHASE_PAYMENT_RECOGNITION"),
                any(EventContext.class)
        )).thenReturn(evaluation);

        when(voucherMapper.selectOne(any())).thenReturn(null);
        BizfiFiVoucher voucher = new BizfiFiVoucher();
        voucher.setFid(900L);
        voucher.setFnumber("V-PAY-001");
        voucher.setTenantId("T1");
        doAnswer(invocation -> {
            BizfiFiVoucher draft = invocation.getArgument(0);
            draft.setFid(900L);
            draft.setFnumber("V-PAY-001");
            return draft;
        }).when(voucherService).saveDraft(any(BizfiFiVoucher.class));
        when(voucherService.saveLines(eq(900L), any())).thenReturn(true);
        when(voucherService.get(900L)).thenReturn(voucher);

        BizfiFiVoucherLine line1 = new BizfiFiVoucherLine();
        line1.setFid(901L);
        line1.setFlineNo(1);
        BizfiFiVoucherLine line2 = new BizfiFiVoucherLine();
        line2.setFid(902L);
        line2.setFlineNo(2);
        when(voucherService.listLines(900L))
                .thenReturn(List.of(line1, line2));

        ProcessingResult result = service.process(rawJson);

        assertEquals(false, result.duplicate());
        assertEquals(900L, result.voucherId());
        assertEquals("V-PAY-001", result.voucherNumber());

        verify(ruleEngine).evaluate(
                eq("PURCHASE_PAYMENT_RECOGNITION"),
                any(EventContext.class));
        verify(settlementRepository).updateSettlementAccounting(
                eq(80L), eq("T1"), any(), eq(900L), eq("V-PAY-001"));
        verify(settlementRepository).insertPaymentAccountingTrace(
                anyLong(), eq("T1"), eq(1L),
                eq("BE-PAY-001"), eq("PAYMENT_COMPLETED"),
                eq("40"), eq("PAYORD-001"),
                eq(80L), eq(40L), eq(50L),
                any(), eq("P0_PURCHASE_PAYMENT_RECOGNITION"),
                eq(1), eq(900L), eq("V-PAY-001")
        );
        verify(commonRepository).markInboxProcessed(
                eq("FI_PAYMENT_COMPLETED_ACCOUNTING_V1"),
                eq("BE-PAY-001"),
                eq(null),
                any(),
                eq(900L),
                eq("V-PAY-001")
        );
    }

    @Test
    void duplicatePaymentCompletedDoesNotCreateAnotherVoucher() {
        InboundAccountingRepository commonRepository =
                mock(InboundAccountingRepository.class);
        PaymentSettlementRepository settlementRepository =
                mock(PaymentSettlementRepository.class);
        AccountingRuleEngine ruleEngine = mock(AccountingRuleEngine.class);
        BizfiFiVoucherService voucherService =
                mock(BizfiFiVoucherService.class);
        BizfiFiVoucherMapper voucherMapper =
                mock(BizfiFiVoucherMapper.class);

        PaymentCompletedAccountingService service =
                new PaymentCompletedAccountingService(
                        new ObjectMapper(),
                        commonRepository,
                        settlementRepository,
                        ruleEngine,
                        voucherService,
                        voucherMapper,
                        "FI_PAYMENT_COMPLETED_ACCOUNTING_V1",
                        "DEFAULT"
                );

        when(commonRepository.findInboxStatus(
                "FI_PAYMENT_COMPLETED_ACCOUNTING_V1", "BE-PAY-001"))
                .thenReturn("PROCESSED");
        when(commonRepository.findInboxResult(
                "FI_PAYMENT_COMPLETED_ACCOUNTING_V1", "BE-PAY-001"))
                .thenReturn(new ProcessingResult(
                        true, null, "AE-PAY-001", 900L, "V-PAY-001"));

        ProcessingResult result = service.process(eventJson("BE-PAY-001"));

        assertEquals(true, result.duplicate());
        assertEquals(900L, result.voucherId());
        verify(ruleEngine, never()).evaluate(any(), any());
        verify(voucherService, never()).saveDraft(any());
        verify(settlementRepository, never()).findSettlement(anyLong(), any());
    }

    private String eventJson(String eventId) {
        return """
                {
                  "eventId":"%s",
                  "eventType":"PAYMENT_COMPLETED",
                  "eventVersion":1,
                  "tenantId":"T1",
                  "orgId":1,
                  "producerService":"fi-service",
                  "domainCode":"FUND",
                  "aggregateType":"PAYMENT_ORDER",
                  "aggregateId":"40",
                  "aggregateVersion":4,
                  "sourceSystemCode":"MATRIX",
                  "sourceDocumentType":"FI_PAYMENT_ORDER",
                  "sourceDocumentId":"40",
                  "sourceDocumentNo":"PAYORD-001",
                  "businessDate":"2026-08-27",
                  "correlationId":"701",
                  "causationId":"TX-001",
                  "operatorId":99,
                  "payload":{
                    "paymentOrderId":40,
                    "paymentOrderNo":"PAYORD-001",
                    "settlementId":80,
                    "settlementNo":"APSET-001",
                    "bankTransactionId":50,
                    "bankTransactionNo":"TX-001",
                    "businessPartnerId":"BP-1",
                    "businessPartnerCode":"SUP-1",
                    "businessPartnerName":"供应商1",
                    "currencyCode":"CNY",
                    "amount":600,
                    "payerBankAccountId":"BANK-001",
                    "settlementDate":"2026-08-27",
                    "entries":[
                      {
                        "settlementEntryId":81,
                        "payableId":101,
                        "payableNumber":"AP-001",
                        "paymentApplicationId":20,
                        "settledAmount":400
                      },
                      {
                        "settlementEntryId":82,
                        "payableId":102,
                        "payableNumber":"AP-002",
                        "paymentApplicationId":20,
                        "settledAmount":200
                      }
                    ]
                  }
                }
                """.formatted(eventId);
    }
}
