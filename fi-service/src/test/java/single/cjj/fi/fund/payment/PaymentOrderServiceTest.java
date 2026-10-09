package single.cjj.fi.fund.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.fund.payment.PaymentOrderContracts.ActionRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.BotpCreateRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.LiquidityCheckRequest;
import single.cjj.fi.fund.payment.PaymentOrderRepository.PaymentApplicationRow;
import single.cjj.fi.fund.payment.PaymentOrderRepository.PaymentOrderAllocationRow;
import single.cjj.fi.fund.payment.PaymentOrderRepository.PaymentOrderRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentOrderServiceTest {

    @Test
    void botpCreateReservesApplicationOrderedAmount() {
        PaymentOrderRepository repository = mock(PaymentOrderRepository.class);
        PaymentOrderService service = new PaymentOrderService(repository, new ObjectMapper());

        PaymentApplicationRow application = application(
                new BigDecimal("1000"), new BigDecimal("100"));
        when(repository.findByIdempotency("T1", "botp:T1:E1:0")).thenReturn(null);
        when(repository.lockApplication(20L, "T1")).thenReturn(application);

        AtomicReference<PaymentOrderRow> inserted = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return null;
        }).when(repository).insertOrder(any(PaymentOrderRow.class));
        when(repository.findOrder(anyLong(), eq("T1")))
                .thenAnswer(invocation -> inserted.get());
        when(repository.findAllocations(anyLong(), eq("T1"))).thenReturn(List.of());

        service.createFromBotp(new BotpCreateRequest(
                "botp:T1:E1:0", "T1", 1L, 20L,
                "MATRIX", "FI_PAYMENT_APPLICATION", "PA:20", "E1",
                new BigDecimal("500"), "BANK_DIRECT",
                LocalDate.of(2026, 8, 30), "BANK-001", 99L
        ));

        verify(repository).updateApplicationOrdered(
                20L, "T1", new BigDecimal("600.00"), "PARTIAL", 99L);
        verify(repository).insertAllocation(any(PaymentOrderAllocationRow.class));
    }

    @Test
    void rejectReleasesOrderedAmount() {
        PaymentOrderRepository repository = mock(PaymentOrderRepository.class);
        PaymentOrderService service = new PaymentOrderService(repository, new ObjectMapper());

        PaymentOrderRow order = order("SUBMITTED", "SUBMITTED", "PENDING");
        PaymentOrderAllocationRow allocation = new PaymentOrderAllocationRow(
                30L, "T1", 1L, 40L, 20L, "PAYAPP-001",
                new BigDecimal("500"), "ORDERED", 99L,
                LocalDateTime.of(2026, 8, 27, 10, 0), 0
        );
        PaymentApplicationRow application = application(
                new BigDecimal("1000"), new BigDecimal("600"));

        when(repository.lockOrder(40L, "T1")).thenReturn(order);
        when(repository.findOrder(40L, "T1")).thenReturn(order);
        when(repository.findAllocations(40L, "T1")).thenReturn(List.of(allocation));
        when(repository.lockApplication(20L, "T1")).thenReturn(application);

        service.reject(40L, "T1", new ActionRequest(99L, "退回"));

        verify(repository).updateApplicationOrdered(
                20L, "T1", new BigDecimal("100.00"), "PARTIAL", 99L);
        verify(repository).updateAllocationStatus(30L, "T1", "RELEASED", 99L);
    }

    @Test
    void auditRequiresLiquidityCheck() {
        PaymentOrderRepository repository = mock(PaymentOrderRepository.class);
        PaymentOrderService service = new PaymentOrderService(repository, new ObjectMapper());

        PaymentOrderRow order = order("SUBMITTED", "SUBMITTED", "PENDING");
        when(repository.lockOrder(40L, "T1")).thenReturn(order);

        assertThrows(
                BizException.class,
                () -> service.audit(40L, "T1", new ActionRequest(99L, null))
        );
        verify(repository, never()).updateOrderState(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void liquidityPassMustCoverPaymentAmount() {
        PaymentOrderRepository repository = mock(PaymentOrderRepository.class);
        PaymentOrderService service = new PaymentOrderService(repository, new ObjectMapper());

        PaymentOrderRow order = order("DRAFT", "DRAFT", "PENDING");
        when(repository.lockOrder(40L, "T1")).thenReturn(order);

        assertThrows(
                BizException.class,
                () -> service.recordLiquidityCheck(
                        40L, "T1",
                        new LiquidityCheckRequest(
                                "PASSED", "LQ-1", new BigDecimal("400"), null, null),
                        99L)
        );
    }

    private PaymentApplicationRow application(BigDecimal amount, BigDecimal ordered) {
        return new PaymentApplicationRow(
                20L, "T1", 1L, "PAYAPP-001", LocalDate.of(2026, 8, 27),
                "BP-1", "SUP-1", "供应商1", "CNY",
                amount, ordered, "FP-1", LocalDate.of(2026, 8, 30),
                "BANK_DIRECT", "PAYEE-1", "供应商1", "招商银行",
                "622200001", "APPROVED", "AUDITED", "PARTIAL", 1
        );
    }

    private PaymentOrderRow order(
            String status,
            String approvalStatus,
            String liquidityStatus
    ) {
        return new PaymentOrderRow(
                40L, "T1", 1L, "PAYORD-001", LocalDate.of(2026, 8, 27),
                "BP-1", "SUP-1", "供应商1", "CNY", new BigDecimal("500"),
                "BANK_DIRECT", "BANK-001", "PAYEE-1",
                "供应商1", "招商银行", "622200001",
                "FP-1", LocalDate.of(2026, 8, 30),
                liquidityStatus, null, null, null,
                status, approvalStatus,
                null, null, "NOT_SENT", null,
                null, null, null, null,
                "UNMATCHED", null, null,
                "botp:T1:E1:0", "MATRIX", "FI_PAYMENT_APPLICATION",
                "PA:20", "E1", null, null, 99L,
                LocalDateTime.of(2026, 8, 27, 10, 0), 0
        );
    }
}
