package single.cjj.fi.expense.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseWorkflowServiceTest {

    @Mock
    private ExpenseWorkflowRepository repository;

    @Mock
    private ExpenseWorkflowGateway gateway;

    @Test
    void draftSubmissionCreatesWorkflowStartOutbox() {
        ExpenseWorkflowRepository.ExpenseRow draft = expense("DRAFT", null, 0);
        ExpenseWorkflowRepository.ExpenseRow approving = expense("APPROVING", null, 1);
        when(repository.findExpense("exp1"))
                .thenReturn(Optional.of(draft))
                .thenReturn(Optional.of(approving));
        when(repository.findBinding("tenant", ExpenseWorkflowService.BUSINESS_TYPE, "exp1"))
                .thenReturn(Optional.empty());
        when(repository.markApproving("exp1", 0, null)).thenReturn(1);

        ExpenseWorkflowService service = service();
        service.submit("exp1", new ExpenseWorkflowContracts.SubmitExpenseRequest(
                "user1", "expense-reimbursement", "submit"), "req1");

        verify(gateway).requireUploadedCategory("tenant", "exp1", "INVOICE", 1);
        ArgumentCaptor<ExpenseWorkflowRepository.OutboxRow> captor =
                ArgumentCaptor.forClass(ExpenseWorkflowRepository.OutboxRow.class);
        verify(repository).insertOutbox(captor.capture());
        assertEquals(ExpenseWorkflowService.EVENT_START, captor.getValue().eventType());
        assertEquals("expense-start:exp1", captor.getValue().idempotencyKey());
    }

    @Test
    void returnedSubmissionCreatesResubmitOutbox() {
        ExpenseWorkflowRepository.ExpenseRow returned = expense("RETURNED", "wf1", 3);
        ExpenseWorkflowRepository.ExpenseRow approving = expense("APPROVING", "wf1", 4);
        ExpenseWorkflowRepository.BindingRow binding = binding("WAITING_RESUBMIT", "RETURNED");
        when(repository.findExpense("exp1"))
                .thenReturn(Optional.of(returned))
                .thenReturn(Optional.of(approving));
        when(repository.findBinding("tenant", ExpenseWorkflowService.BUSINESS_TYPE, "exp1"))
                .thenReturn(Optional.of(binding));
        when(repository.markApproving("exp1", 3, "wf1")).thenReturn(1);

        ExpenseWorkflowService service = service();
        service.submit("exp1", new ExpenseWorkflowContracts.SubmitExpenseRequest(
                "user1", null, "fixed"), "req2");

        ArgumentCaptor<ExpenseWorkflowRepository.OutboxRow> captor =
                ArgumentCaptor.forClass(ExpenseWorkflowRepository.OutboxRow.class);
        verify(repository).insertOutbox(captor.capture());
        assertEquals(ExpenseWorkflowService.EVENT_RESUBMIT, captor.getValue().eventType());
        assertEquals("expense-resubmit:exp1:req2", captor.getValue().idempotencyKey());
    }

    @Test
    void returnedExpenseCanBeEditedWithOptimisticVersion() {
        ExpenseWorkflowRepository.ExpenseRow returned = expense("RETURNED", "wf1", 3);
        ExpenseWorkflowRepository.ExpenseRow updated = new ExpenseWorkflowRepository.ExpenseRow(
                "exp1", "tenant", "ER-1", "user1", "D002", new BigDecimal("120.00"),
                "CNY", "updated", "RETURNED", "wf1", 4,
                LocalDateTime.now(), null, null);
        when(repository.findExpense("exp1"))
                .thenReturn(Optional.of(returned))
                .thenReturn(Optional.of(updated));
        when(repository.updateEditable("exp1", "user1", 3, "D002",
                new BigDecimal("120.00"), "CNY", "updated")).thenReturn(1);

        ExpenseWorkflowContracts.ExpenseResponse response = service().update("exp1",
                new ExpenseWorkflowContracts.UpdateExpenseRequest(
                        "user1", "D002", new BigDecimal("120.00"), "CNY", "updated", 3));

        assertEquals(4, response.version());
        assertEquals("D002", response.departmentCode());
    }

    @Test
    void approvingExpenseCreatesCancelOutbox() {
        ExpenseWorkflowRepository.ExpenseRow approving = expense("APPROVING", "wf1", 2);
        when(repository.findExpense("exp1")).thenReturn(Optional.of(approving));
        when(repository.findBinding("tenant", ExpenseWorkflowService.BUSINESS_TYPE, "exp1"))
                .thenReturn(Optional.of(binding("RUNNING", "APPROVING")));
        when(repository.markCancelRequested("tenant", "exp1", "wf1")).thenReturn(1);

        service().cancel("exp1", new ExpenseWorkflowContracts.CancelExpenseRequest(
                "user1", "no longer needed"), "cancel-1");

        ArgumentCaptor<ExpenseWorkflowRepository.OutboxRow> captor =
                ArgumentCaptor.forClass(ExpenseWorkflowRepository.OutboxRow.class);
        verify(repository).insertOutbox(captor.capture());
        assertEquals(ExpenseWorkflowService.EVENT_CANCEL, captor.getValue().eventType());
        assertEquals("expense-cancel:exp1:cancel-1", captor.getValue().idempotencyKey());
    }

    private ExpenseWorkflowService service() {
        return new ExpenseWorkflowService(
                repository, gateway, new ObjectMapper(),
                "http://fi/api/fi/expense/workflow/events");
    }

    private ExpenseWorkflowRepository.BindingRow binding(String workflowStatus,
                                                          String businessStatus) {
        return new ExpenseWorkflowRepository.BindingRow(
                "binding1", "tenant", ExpenseWorkflowService.BUSINESS_TYPE, "exp1", "wf1",
                "expense-reimbursement", workflowStatus, businessStatus,
                "old", "callback", "user1");
    }

    private ExpenseWorkflowRepository.ExpenseRow expense(
            String status, String workflowInstanceId, int version) {
        return new ExpenseWorkflowRepository.ExpenseRow(
                "exp1", "tenant", "ER-1", "user1", "D001", new BigDecimal("100.00"),
                "CNY", "travel", status, workflowInstanceId, version,
                LocalDateTime.now(), null, null);
    }
}
