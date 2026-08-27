package single.cjj.fi.ap.settlement;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import single.cjj.fi.ap.settlement.PaymentSettlementContracts.Detail;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.ApplicationAllocationRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.BankTransactionRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.OrderAllocationRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.PayableRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.PaymentApplicationRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.PaymentOrderRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.SettlementEntryRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.SettlementRow;
import single.cjj.fi.event.FiBusinessEventOutboxEntity;
import single.cjj.fi.event.FiBusinessEventOutboxService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class PaymentSettlementServiceTest {

    @Test
    void shouldConsumeReservationsPartiallyAndMarkOrderPaidAtomically() {
        PaymentSettlementRepository repository = mock(PaymentSettlementRepository.class);
        FiBusinessEventOutboxService outbox = mock(FiBusinessEventOutboxService.class);
        PaymentSettlementService service = new PaymentSettlementService(repository, outbox);

        PaymentOrderRow order = new PaymentOrderRow(
                40L, "T1", 1L, "PAYORD-001", LocalDate.of(2026, 8, 27),
                "BP-1", "SUP-1", "供应商1", "CNY", new BigDecimal("600"),
                "BANK-001", "PAYING", "AUDITED", "CONFIRMED", "MATCHED",
                700L, 701L, null, 3
        );
        BankTransactionRow bank = new BankTransactionRow(
                50L, "T1", 1L, "BANK-001", "TX-001",
                LocalDate.of(2026, 8, 27),
                LocalDateTime.of(2026, 8, 27, 10, 30),
                "OUTBOUND", "CNY", new BigDecimal("600"),
                "MATCHED", "CONFIRMED", 40L, 700L, 701L, 1
        );
        OrderAllocationRow orderAllocation = new OrderAllocationRow(
                60L, "T1", 1L, 40L, 20L,
                new BigDecimal("600"), "ORDERED", 0
        );
        PaymentApplicationRow application = new PaymentApplicationRow(
                20L, "T1", 1L, "PAYAPP-001",
                new BigDecimal("1000"), new BigDecimal("1000"),
                "BP-1", "CNY", "APPROVED", "AUDITED", "COMPLETE", 2
        );
        ApplicationAllocationRow alloc1 = new ApplicationAllocationRow(
                201L, "T1", 1L, 20L, 101L, "AP-001",
                new BigDecimal("400"), new BigDecimal("400"), BigDecimal.ZERO,
                "RESERVED", 0
        );
        ApplicationAllocationRow alloc2 = new ApplicationAllocationRow(
                202L, "T1", 1L, 20L, 102L, "AP-002",
                new BigDecimal("600"), new BigDecimal("600"), BigDecimal.ZERO,
                "RESERVED", 0
        );
        PayableRow payable1 = new PayableRow(
                101L, "T1", 1L, "AP-001", "FORMAL",
                "BP-1", "CNY", new BigDecimal("400"),
                new BigDecimal("400"), BigDecimal.ZERO, new BigDecimal("400"),
                "OPEN", "AUDITED", "VOUCHER_GENERATED", 1
        );
        PayableRow payable2 = new PayableRow(
                102L, "T1", 1L, "AP-002", "FORMAL",
                "BP-1", "CNY", new BigDecimal("600"),
                new BigDecimal("600"), BigDecimal.ZERO, new BigDecimal("600"),
                "OPEN", "AUDITED", "VOUCHER_GENERATED", 1
        );

        when(repository.findByPaymentOrder(40L, "T1")).thenReturn(null);
        when(repository.lockPaymentOrder(40L, "T1")).thenReturn(order);
        when(repository.lockMatchedBankTransaction(40L, "T1")).thenReturn(bank);
        when(repository.lockOrderAllocations(40L, "T1"))
                .thenReturn(List.of(orderAllocation));
        when(repository.lockApplication(20L, "T1")).thenReturn(application);
        when(repository.lockAvailableApplicationAllocations(20L, "T1"))
                .thenReturn(List.of(alloc1, alloc2));
        when(repository.lockPayable(101L, "T1")).thenReturn(payable1);
        when(repository.lockPayable(102L, "T1")).thenReturn(payable2);

        AtomicReference<SettlementRow> insertedSettlement = new AtomicReference<>();
        List<SettlementEntryRow> insertedEntries = new ArrayList<>();
        doAnswer(invocation -> {
            insertedSettlement.set(invocation.getArgument(0));
            return null;
        }).when(repository).insertSettlement(any(SettlementRow.class));
        doAnswer(invocation -> {
            insertedEntries.add(invocation.getArgument(0));
            return null;
        }).when(repository).insertSettlementEntry(any(SettlementEntryRow.class));
        when(repository.findSettlement(anyLong(), eq("T1")))
                .thenAnswer(invocation -> insertedSettlement.get());
        when(repository.findSettlementEntries(anyLong(), eq("T1")))
                .thenAnswer(invocation -> List.copyOf(insertedEntries));

        FiBusinessEventOutboxEntity event = new FiBusinessEventOutboxEntity();
        event.setFeventId("BE-PAY-001");
        when(outbox.append(
                eq("T1"), eq(1L), eq("PAYMENT_COMPLETED"), eq("FUND"),
                eq("PAYMENT_ORDER"), eq(40L), anyLong(),
                eq("FI_PAYMENT_ORDER"), eq("PAYORD-001"),
                eq(LocalDate.of(2026, 8, 27)),
                eq("701"), eq("TX-001"), eq(null), eq(99L),
                eq("biz.finance.payment.completed"), any()
        )).thenReturn(event);

        Detail result = service.finalizePayment(40L, "T1", 99L);

        assertEquals(0, result.amount().compareTo(new BigDecimal("600.00")));
        assertEquals(2, result.entries().size());

        verify(repository).updatePayableBalances(
                101L, "T1",
                new BigDecimal("400.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                "SETTLED", 99L
        );
        verify(repository).updatePayableBalances(
                102L, "T1",
                new BigDecimal("200.00"),
                new BigDecimal("400.00"),
                new BigDecimal("400.00"),
                "PARTIAL_SETTLED", 99L
        );
        verify(repository).updateApplicationAllocationConsumed(
                201L, "T1", new BigDecimal("400.00"), "CONSUMED", 99L);
        verify(repository).updateApplicationAllocationConsumed(
                202L, "T1", new BigDecimal("200.00"), "RESERVED", 99L);
        verify(repository).markOrderAllocationConsumed(60L, "T1", 99L);
        verify(repository).markPaymentOrderPaid(
                eq(40L), eq("T1"), anyLong(), eq(99L));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outbox).append(
                eq("T1"), eq(1L), eq("PAYMENT_COMPLETED"), eq("FUND"),
                eq("PAYMENT_ORDER"), eq(40L), anyLong(),
                eq("FI_PAYMENT_ORDER"), eq("PAYORD-001"),
                eq(LocalDate.of(2026, 8, 27)),
                eq("701"), eq("TX-001"), eq(null), eq(99L),
                eq("biz.finance.payment.completed"), payloadCaptor.capture()
        );
        Map<?, ?> payload = (Map<?, ?>) payloadCaptor.getValue();
        assertEquals(new BigDecimal("600.00"), payload.get("amount"));
        assertEquals(2, ((List<?>) payload.get("entries")).size());
    }

    @Test
    void repeatedFinalizeReturnsExistingSettlementWithoutMutatingBalances() {
        PaymentSettlementRepository repository = mock(PaymentSettlementRepository.class);
        FiBusinessEventOutboxService outbox = mock(FiBusinessEventOutboxService.class);
        PaymentSettlementService service = new PaymentSettlementService(repository, outbox);

        SettlementRow existing = new SettlementRow(
                80L, "T1", 1L, "APSET-001", 40L, 50L,
                "BP-1", "SUP-1", "供应商1", "CNY",
                new BigDecimal("600"), "COMPLETED",
                LocalDate.of(2026, 8, 27), "BE-PAY-001",
                null, null, null, 99L,
                LocalDateTime.of(2026, 8, 27, 10, 31), 1
        );
        when(repository.findByPaymentOrder(40L, "T1")).thenReturn(existing);
        when(repository.findSettlement(80L, "T1")).thenReturn(existing);
        when(repository.findSettlementEntries(80L, "T1")).thenReturn(List.of());

        Detail result = service.finalizePayment(40L, "T1", 99L);

        assertEquals(80L, result.fid());
        verify(repository, never()).lockPaymentOrder(anyLong(), any());
        verify(outbox, never()).append(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
