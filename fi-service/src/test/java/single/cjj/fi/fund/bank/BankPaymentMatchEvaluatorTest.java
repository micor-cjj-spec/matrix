package single.cjj.fi.fund.bank;

import org.junit.jupiter.api.Test;
import single.cjj.fi.fund.bank.BankTransactionContracts.Difference;
import single.cjj.fi.fund.bank.BankTransactionRepository.BankTransactionRow;
import single.cjj.fi.fund.payment.PaymentOrderRepository.PaymentOrderRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankPaymentMatchEvaluatorTest {

    private final BankPaymentMatchEvaluator evaluator = new BankPaymentMatchEvaluator();

    @Test
    void exactBankPaymentMatches() {
        List<Difference> differences = evaluator.evaluate(
                order(),
                bank("OUTBOUND", "BANK-001", "CNY", "500", "622200001")
        );
        assertTrue(differences.isEmpty());
    }

    @Test
    void amountCurrencyAndAccountDifferencesAreBlocking() {
        List<Difference> differences = evaluator.evaluate(
                order(),
                bank("OUTBOUND", "BANK-X", "USD", "499", "622299999")
        );
        assertEquals(4, differences.size());
        assertTrue(differences.stream()
                .anyMatch(item -> "BANK_ACCOUNT_DIFFERENCE".equals(item.code())));
        assertTrue(differences.stream()
                .anyMatch(item -> "CURRENCY_DIFFERENCE".equals(item.code())));
        assertTrue(differences.stream()
                .anyMatch(item -> "AMOUNT_DIFFERENCE".equals(item.code())));
        assertTrue(differences.stream()
                .anyMatch(item -> "COUNTERPARTY_ACCOUNT_DIFFERENCE".equals(item.code())));
    }

    private PaymentOrderRow order() {
        return new PaymentOrderRow(
                40L, "T1", 1L, "PAYORD-001", LocalDate.of(2026, 8, 27),
                "BP-1", "SUP-1", "供应商1", "CNY", new BigDecimal("500"),
                "BANK_DIRECT", "BANK-001", "PAYEE-1",
                "供应商1", "招商银行", "622200001",
                "FP-1", LocalDate.of(2026, 8, 30),
                "PASSED", "LQ-1", new BigDecimal("1000"), null,
                "PAYING", "AUDITED",
                "BANK_DIRECT", "REQ-1", "SUBMITTED", null,
                LocalDateTime.of(2026, 8, 27, 10, 1), 99L,
                LocalDateTime.of(2026, 8, 27, 10, 2),
                LocalDateTime.of(2026, 8, 27, 10, 3),
                "UNMATCHED", null, null,
                "botp:T1:E1:0", "MATRIX", "FI_PAYMENT_APPLICATION",
                "PA:20", "E1", null, null, 99L,
                LocalDateTime.of(2026, 8, 27, 10, 0), 1
        );
    }

    private BankTransactionRow bank(
            String direction,
            String bankAccountId,
            String currency,
            String amount,
            String counterpartyAccount
    ) {
        return new BankTransactionRow(
                50L, "T1", 1L, bankAccountId, "TX-001",
                LocalDate.of(2026, 8, 27),
                LocalDateTime.of(2026, 8, 27, 10, 5),
                direction, currency, new BigDecimal(amount),
                "供应商1", counterpartyAccount, "货款", "采购付款",
                "RCPT-1", "BANK_DIRECT",
                "UNMATCHED", "CONFIRMED",
                null, null, null, null, null,
                99L, LocalDateTime.of(2026, 8, 27, 10, 5), 0
        );
    }
}
