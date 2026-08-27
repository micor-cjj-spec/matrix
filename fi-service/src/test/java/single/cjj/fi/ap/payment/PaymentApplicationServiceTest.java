package single.cjj.fi.ap.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.ActionRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.AllocationRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.CreateRequest;
import single.cjj.fi.ap.payment.PaymentApplicationRepository.AllocationRow;
import single.cjj.fi.ap.payment.PaymentApplicationRepository.ApplicationRow;
import single.cjj.fi.ap.payment.PaymentApplicationRepository.PayableRow;
import single.cjj.fi.event.FiBusinessEventOutboxService;

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

class PaymentApplicationServiceTest {

    @Test
    void createReservesFormalPayableWithoutReducingOpenAmount() {
        PaymentApplicationRepository repository = mock(PaymentApplicationRepository.class);
        FiBusinessEventOutboxService outbox = mock(FiBusinessEventOutboxService.class);
        PaymentApplicationService service = new PaymentApplicationService(
                repository, outbox, new ObjectMapper());

        PayableRow payable = payable(new BigDecimal("1000"), BigDecimal.ZERO);
        when(repository.lockPayable(10L, "T1")).thenReturn(payable);
        when(repository.findPayable(10L, "T1")).thenReturn(payable);
        when(repository.findEvidence(anyLong(), eq("T1"))).thenReturn(List.of());
        when(repository.findAllocations(anyLong(), eq("T1"))).thenReturn(List.of());

        AtomicReference<ApplicationRow> inserted = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return null;
        }).when(repository).insertApplication(any(ApplicationRow.class));
        when(repository.findApplication(anyLong(), eq("T1")))
                .thenAnswer(invocation -> inserted.get());

        service.create(new CreateRequest(
                "T1", 1L, 99L, LocalDate.of(2026, 8, 27),
                LocalDate.of(2026, 8, 30), "BANK_DIRECT", "FP-001",
                null, null, null, null, "test",
                List.of(new AllocationRequest(10L, new BigDecimal("600"))),
                List.of()
        ), 99L);

        verify(repository).setPayableReserved(
                10L, "T1", new BigDecimal("600.00"), 99L);
        // PaymentApplication only reserves. It must not change payable.openAmount.
        verify(repository, never()).updateApplicationState(
                eq(10L), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitIsBlockedUntilEvidenceAndBudgetControlsPass() {
        PaymentApplicationRepository repository = mock(PaymentApplicationRepository.class);
        FiBusinessEventOutboxService outbox = mock(FiBusinessEventOutboxService.class);
        PaymentApplicationService service = new PaymentApplicationService(
                repository, outbox, new ObjectMapper());

        ApplicationRow app = app("DRAFT", "DRAFT", "PENDING", "PENDING");
        when(repository.lockApplication(20L, "T1")).thenReturn(app);
        when(repository.findAllocations(20L, "T1")).thenReturn(List.of(
                allocation("RESERVED", new BigDecimal("600"))
        ));

        assertThrows(BizException.class,
                () -> service.submit(20L, "T1", new ActionRequest(99L, null)));
        verify(repository, never()).updateApplicationState(
                anyLong(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectReleasesReservationBackToPayable() {
        PaymentApplicationRepository repository = mock(PaymentApplicationRepository.class);
        FiBusinessEventOutboxService outbox = mock(FiBusinessEventOutboxService.class);
        PaymentApplicationService service = new PaymentApplicationService(
                repository, outbox, new ObjectMapper());

        ApplicationRow submitted = app("SUBMITTED", "SUBMITTED", "PASSED", "PASSED");
        PayableRow payable = payable(new BigDecimal("1000"), new BigDecimal("600"));
        when(repository.lockApplication(20L, "T1")).thenReturn(submitted);
        when(repository.findApplication(20L, "T1")).thenReturn(submitted);
        when(repository.findAllocations(20L, "T1")).thenReturn(List.of(
                allocation("RESERVED", new BigDecimal("600"))
        ));
        when(repository.findEvidence(20L, "T1")).thenReturn(List.of());
        when(repository.lockPayable(10L, "T1")).thenReturn(payable);

        service.reject(20L, "T1", new ActionRequest(99L, "资料退回"));

        verify(repository).setPayableReserved(
                10L, "T1", new BigDecimal("0.00"), 99L);
        verify(repository).updateAllocationReservation(
                30L, "T1", BigDecimal.ZERO, "RELEASED", 99L);
    }

    @Test
    void approveKeepsReservationAndAppendsApprovedBusinessEvent() {
        PaymentApplicationRepository repository = mock(PaymentApplicationRepository.class);
        FiBusinessEventOutboxService outbox = mock(FiBusinessEventOutboxService.class);
        PaymentApplicationService service = new PaymentApplicationService(
                repository, outbox, new ObjectMapper());

        ApplicationRow submitted = app("SUBMITTED", "SUBMITTED", "PASSED", "PASSED");
        ApplicationRow approved = app("APPROVED", "AUDITED", "PASSED", "PASSED");
        AllocationRow allocation = allocation("RESERVED", new BigDecimal("600"));

        when(repository.lockApplication(20L, "T1")).thenReturn(submitted);
        when(repository.findApplication(20L, "T1")).thenReturn(approved);
        when(repository.findAllocations(20L, "T1")).thenReturn(List.of(allocation));
        when(repository.findEvidence(20L, "T1")).thenReturn(List.of());

        service.approve(20L, "T1", new ActionRequest(99L, null));

        verify(repository, never()).setPayableReserved(anyLong(), any(), any(), any());
        verify(outbox).append(
                eq("T1"), eq(1L), eq("PAYMENT_APPLICATION_APPROVED"),
                eq("AP"), eq("PAYMENT_APPLICATION"), eq(20L), anyLong(),
                eq("FI_PAYMENT_APPLICATION"), eq("PAYAPP-001"),
                eq(LocalDate.of(2026, 8, 27)),
                any(), any(), any(), eq(99L),
                eq("biz.finance.payment_application.approved"), any()
        );
    }

    private PayableRow payable(BigDecimal open, BigDecimal reserved) {
        return new PayableRow(
                10L, "T1", 1L, "AP-001", "FORMAL",
                LocalDate.of(2026, 8, 27), "BP-1", "SUP-1", "供应商1",
                "CNY", new BigDecimal("1000"), open, BigDecimal.ZERO, reserved,
                "OPEN", "AUDITED", "VOUCHER_GENERATED", 0
        );
    }

    private AllocationRow allocation(String status, BigDecimal amount) {
        return new AllocationRow(
                30L, "T1", 1L, 20L, 10L, "AP-001",
                amount, amount, BigDecimal.ZERO.setScale(2), status,
                99L, LocalDateTime.of(2026, 8, 27, 9, 0), 0
        );
    }

    private ApplicationRow app(
            String status,
            String approvalStatus,
            String evidenceStatus,
            String budgetStatus
    ) {
        return new ApplicationRow(
                20L, "T1", 1L, "PAYAPP-001", LocalDate.of(2026, 8, 27), 99L,
                "BP-1", "SUP-1", "供应商1", "CNY", new BigDecimal("600"),
                "FP-001", LocalDate.of(2026, 8, 30), "BANK_DIRECT",
                null, null, null, null,
                evidenceStatus, budgetStatus, "BUD-001", new BigDecimal("1000"), null,
                status, approvalStatus, "NOT_EXECUTED",
                "botp:T1:BOTP-1:0", "MATRIX", "FI_AP_PAYABLE", "10", "BOTP-1",
                null, null, null, null, 99L,
                LocalDateTime.of(2026, 8, 27, 9, 0), 0
        );
    }
}
