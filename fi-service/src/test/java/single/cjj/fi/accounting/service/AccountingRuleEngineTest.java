package single.cjj.fi.accounting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.accounting.model.AccountingModels.AccountMappingCandidate;
import single.cjj.fi.accounting.model.AccountingModels.EventContext;
import single.cjj.fi.accounting.model.AccountingModels.RuleEntry;
import single.cjj.fi.accounting.model.AccountingModels.RuleEvaluation;
import single.cjj.fi.accounting.model.AccountingModels.RuleHeader;
import single.cjj.fi.accounting.persistence.InboundAccountingRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountingRuleEngineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldResolveControlledAmountExpressions() throws Exception {
        AccountingRuleEngine engine = new AccountingRuleEngine(mock(InboundAccountingRepository.class));
        JsonNode payload = objectMapper.readTree("""
                {"totalAmount":100,"entries":[{"amount":60},{"amount":40}]}
                """);

        assertEquals(0, engine.resolveAmount("FIELD(entry.amount)", payload, payload.path("entries").get(0))
                .compareTo(new BigDecimal("60")));
        assertEquals(0, engine.resolveAmount("FIELD(payload.totalAmount)", payload, null)
                .compareTo(new BigDecimal("100")));
        assertEquals(0, engine.resolveAmount("SUM(entries.amount)", payload, null)
                .compareTo(new BigDecimal("100")));
        assertThrows(BizException.class, () -> engine.resolveAmount("EVAL(payload.totalAmount)", payload, null));
    }

    @Test
    void shouldRejectAmbiguousHighestRankRules() throws Exception {
        InboundAccountingRepository repository = mock(InboundAccountingRepository.class);
        AccountingRuleEngine engine = new AccountingRuleEngine(repository);
        LocalDate date = LocalDate.of(2026, 8, 26);
        JsonNode payload = objectMapper.readTree("{\"totalAmount\":100,\"entries\":[{\"amount\":100}]}");
        EventContext context = new EventContext("T001", 1L, "DEFAULT", date, "PI001", payload);

        when(repository.findPublishedRules("T001", 1L, "PURCHASE_INBOUND_ESTIMATE_RECOGNITION", "DEFAULT", date))
                .thenReturn(List.of(
                        new RuleHeader(1L, 11L, "R1", 1, 10, 3, "FORMAL", "DEFAULT"),
                        new RuleHeader(2L, 12L, "R2", 1, 10, 3, "FORMAL", "DEFAULT")
                ));

        BizException exception = assertThrows(BizException.class,
                () -> engine.evaluate("PURCHASE_INBOUND_ESTIMATE_RECOGNITION", context));
        assertEquals(true, exception.getMessage().startsWith("ACCOUNTING_RULE_CONFLICT"));
    }

    @Test
    void shouldSkipExplicitZeroAmountRuleEntry() throws Exception {
        InboundAccountingRepository repository = mock(InboundAccountingRepository.class);
        AccountingRuleEngine engine = new AccountingRuleEngine(repository);
        LocalDate date = LocalDate.of(2026, 8, 27);
        JsonNode payload = objectMapper.readTree("""
                {
                  "grossAmount":100,
                  "currencyCode":"CNY",
                  "entries":[{"supplierInvoiceEntryId":"S1","netAmount":100,"taxAmount":0}]
                }
                """);
        EventContext context = new EventContext("T001", 1L, "DEFAULT", date, "SI001", payload);
        RuleHeader header = new RuleHeader(1L, 11L, "PURCHASE_AP", 1, 10, 3, "FORMAL", "DEFAULT");

        when(repository.findPublishedRules("T001", 1L, "PURCHASE_AP_RECOGNITION", "DEFAULT", date))
                .thenReturn(List.of(header));
        when(repository.findRuleEntries(11L)).thenReturn(List.of(
                new RuleEntry(101L, 1, "ENTRY", "DEBIT", "MAPPING", "PURCHASE_INVOICE_DEBIT", null,
                        "FIELD(entry.netAmount)", false, "采购发票", "FIELD(payload.currencyCode)"),
                new RuleEntry(102L, 2, "ENTRY", "DEBIT", "MAPPING", "INPUT_VAT", null,
                        "FIELD(entry.taxAmount)", true, "进项税", "FIELD(payload.currencyCode)"),
                new RuleEntry(103L, 3, "HEADER", "CREDIT", "MAPPING", "FORMAL_AP", null,
                        "FIELD(payload.grossAmount)", false, "正式应付", "FIELD(payload.currencyCode)")
        ));
        when(repository.findRuleDimensions(11L)).thenReturn(List.of());
        when(repository.findAccountMappings(anyString(), any(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    String accountKey = invocation.getArgument(3);
                    String accountCode = "FORMAL_AP".equals(accountKey) ? "2202" : "1405";
                    return List.of(new AccountMappingCandidate(1L, accountCode, 10, 3, "TEST"));
                });

        RuleEvaluation result = engine.evaluate("PURCHASE_AP_RECOGNITION", context);

        assertEquals(2, result.lines().size());
        assertEquals(0, result.debitTotal().compareTo(new BigDecimal("100.00")));
        assertEquals(0, result.creditTotal().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void shouldBuildBalancedEntryDebitAndHeaderCredit() throws Exception {
        InboundAccountingRepository repository = mock(InboundAccountingRepository.class);
        AccountingRuleEngine engine = new AccountingRuleEngine(repository);
        LocalDate date = LocalDate.of(2026, 8, 26);
        JsonNode payload = objectMapper.readTree("""
                {
                  "totalAmount":100,
                  "currencyCode":"CNY",
                  "entries":[
                    {"inboundEntryId":"I1","amount":60,"materialName":"A"},
                    {"inboundEntryId":"I2","amount":40,"materialName":"B"}
                  ]
                }
                """);
        EventContext context = new EventContext("T001", 1L, "DEFAULT", date, "PI001", payload);
        RuleHeader header = new RuleHeader(1L, 11L, "PURCHASE_INBOUND_ESTIMATE", 1, 10, 3, "FORMAL", "DEFAULT");

        when(repository.findPublishedRules("T001", 1L, "PURCHASE_INBOUND_ESTIMATE_RECOGNITION", "DEFAULT", date))
                .thenReturn(List.of(header));
        when(repository.findRuleEntries(11L)).thenReturn(List.of(
                new RuleEntry(101L, 1, "ENTRY", "DEBIT", "MAPPING", "PURCHASE_INBOUND_DEBIT", null,
                        "FIELD(entry.amount)", false, "入库-${materialName}", "FIELD(payload.currencyCode)"),
                new RuleEntry(102L, 2, "HEADER", "CREDIT", "MAPPING", "ESTIMATED_AP", null,
                        "FIELD(payload.totalAmount)", false, "暂估-${sourceDocumentNo}", "FIELD(payload.currencyCode)")
        ));
        when(repository.findRuleDimensions(11L)).thenReturn(List.of());
        when(repository.findAccountMappings(anyString(), any(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    String accountKey = invocation.getArgument(3);
                    String accountCode = "ESTIMATED_AP".equals(accountKey) ? "2202" : "1405";
                    return List.of(new AccountMappingCandidate(1L, accountCode, 10, 3, "TEST"));
                });

        RuleEvaluation result = engine.evaluate("PURCHASE_INBOUND_ESTIMATE_RECOGNITION", context);

        assertEquals(3, result.lines().size());
        assertEquals(0, result.debitTotal().compareTo(new BigDecimal("100.00")));
        assertEquals(0, result.creditTotal().compareTo(new BigDecimal("100.00")));
        assertEquals(List.of("1405", "1405", "2202"), result.lines().stream().map(line -> line.accountCode()).toList());
    }
    @Test
    void shouldBuildBalancedPurchasePaymentRecognition() throws Exception {
        InboundAccountingRepository repository = mock(InboundAccountingRepository.class);
        AccountingRuleEngine engine = new AccountingRuleEngine(repository);
        LocalDate date = LocalDate.of(2026, 8, 27);
        JsonNode payload = objectMapper.readTree("""
                {
                  "paymentOrderId":40,
                  "paymentOrderNo":"PAYORD-001",
                  "businessPartnerId":"BP-1",
                  "currencyCode":"CNY",
                  "amount":600,
                  "payerBankAccountId":"BANK-001",
                  "entries":[
                    {"payableId":101,"settledAmount":400},
                    {"payableId":102,"settledAmount":200}
                  ]
                }
                """);
        EventContext context = new EventContext(
                "T001", 1L, "DEFAULT", date, "PAYORD-001", payload);
        RuleHeader header = new RuleHeader(
                1L, 11L, "P0_PURCHASE_PAYMENT_RECOGNITION",
                1, 100, 1, "FORMAL", "DEFAULT");

        when(repository.findPublishedRules(
                "T001", 1L, "PURCHASE_PAYMENT_RECOGNITION", "DEFAULT", date))
                .thenReturn(List.of(header));
        when(repository.findRuleEntries(11L)).thenReturn(List.of(
                new RuleEntry(
                        101L, 10, "HEADER", "DEBIT",
                        "MAPPING", "FORMAL_AP", null,
                        "FIELD(payload.amount)", false,
                        "采购付款-${sourceDocumentNo}",
                        "FIELD(payload.currencyCode)"),
                new RuleEntry(
                        102L, 20, "HEADER", "CREDIT",
                        "MAPPING", "BANK_DEPOSIT", null,
                        "FIELD(payload.amount)", false,
                        "采购付款-${sourceDocumentNo}",
                        "FIELD(payload.currencyCode)")
        ));
        when(repository.findRuleDimensions(11L)).thenReturn(List.of());
        when(repository.findAccountMappings(
                anyString(), any(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    String accountKey = invocation.getArgument(3);
                    String accountCode = "FORMAL_AP".equals(accountKey)
                            ? "2202"
                            : "1002";
                    return List.of(new AccountMappingCandidate(
                            1L, accountCode, 10, 3, "TEST"));
                });

        RuleEvaluation result = engine.evaluate(
                "PURCHASE_PAYMENT_RECOGNITION", context);

        assertEquals(2, result.lines().size());
        assertEquals("DEBIT", result.lines().get(0).direction());
        assertEquals("2202", result.lines().get(0).accountCode());
        assertEquals("CREDIT", result.lines().get(1).direction());
        assertEquals("1002", result.lines().get(1).accountCode());
        assertEquals(
                0,
                result.debitTotal().compareTo(new BigDecimal("600.00")));
        assertEquals(
                0,
                result.creditTotal().compareTo(new BigDecimal("600.00")));
    }

}
