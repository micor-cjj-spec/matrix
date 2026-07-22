package single.cjj.fi.expense.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseWorkflowCallbackServiceTest {

    @Mock
    private ExpenseWorkflowRepository repository;

    @Test
    void completedEventApprovesExpense() {
        when(repository.insertWorkflowEventIfAbsent(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(repository.applyWorkflowEvent(
                "tenant", "exp1", "wf1", "APPROVED", "COMPLETED", true)).thenReturn(1);

        ExpenseWorkflowCallbackService service = new ExpenseWorkflowCallbackService(repository);
        boolean processed = service.process(event("evt1", "INSTANCE_COMPLETED"));

        assertTrue(processed);
        verify(repository).markWorkflowEventProcessed("evt1");
    }

    @Test
    void duplicateEventIsIgnored() {
        when(repository.insertWorkflowEventIfAbsent(org.mockito.ArgumentMatchers.any())).thenReturn(0);

        ExpenseWorkflowCallbackService service = new ExpenseWorkflowCallbackService(repository);
        assertFalse(service.process(event("evt1", "INSTANCE_RETURNED")));
    }

    private ExpenseWorkflowContracts.WorkflowEventRequest event(String eventId, String eventType) {
        return new ExpenseWorkflowContracts.WorkflowEventRequest(
                eventId, eventType, "wf1", "tenant", ExpenseWorkflowService.SOURCE_SYSTEM,
                ExpenseWorkflowService.BUSINESS_TYPE, "exp1", "COMPLETED", Map.of(), null);
    }
}
