package single.cjj.fi.fund.bank;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import single.cjj.fi.fund.bank.BankTransactionContracts.MatchRequest;
import single.cjj.fi.fund.bank.BankTransactionContracts.MatchResult;
import single.cjj.fi.fund.bank.BankTransactionRepository.BankTransactionRow;
import single.cjj.fi.fund.payment.PaymentOrderRepository;
import single.cjj.fi.fund.payment.PaymentOrderRepository.PaymentOrderRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BankTransactionServiceTest {

    @Test
    void matchedBankTransactionDoesNotMarkPaymentOrderPaid() {
        BankTransactionRepository bankRepository = mock(BankTransactionRepository.class);
        PaymentOrderRepository paymentRepository = mock(PaymentOrderRepository.class);
        BankTransactionService service = new BankTransactionService(
                bankRepository, paymentRepository, new ObjectMapper());

        BankTransactionRow bank = bank();
        PaymentOrderRow order = order();

        when(bankRepository.lock(50L, "T1")).thenReturn(bank);
        when(paymentRepository.lockOrder(40L, "T1")).thenReturn(order);

        MatchResult result = service.matchPaymentOrder(
                50L, "T1", new MatchRequest(40L, 99L));

        assertEquals("MATCHED", result.result());
        verify(bankRepository).updateMatch(
                eq(50L), eq("T1"), eq("MATCHED"), eq(40L),
                anyLong(), anyLong(), eq(99L));
        verify(paymentRepository).updateOrderBankMatch(
                eq(40L), eq("T1"), eq("MATCHED"),
                anyLong(), anyLong(), eq(99L));

        // P0-IMP-07 only records bank matching.
        // PAID is reserved for P0-IMP-08 settlement transaction.
        verify(paymentRepository, never()).updateOrderState(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any());
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

    private BankTransactionRow bank() {
        return new BankTransactionRow(
                50L, "T1", 1L, "BANK-001", "TX-001",
                LocalDate.of(2026, 8, 27),
                LocalDateTime.of(2026, 8, 27, 10, 5),
                "OUTBOUND", "CNY", new BigDecimal("500"),
                "供应商1", "622200001", "货款", "采购付款",
                "RCPT-1", "BANK_DIRECT",
                "UNMATCHED", "CONFIRMED",
                null, null, null, null, null,
                99L, LocalDateTime.of(2026, 8, 27, 10, 5), 0
        );
    }
}
