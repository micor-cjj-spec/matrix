package single.cjj.fi.expense.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseWorkflowReconciliationServiceTest {

    @Mock
    private ExpenseWorkflowRepository repository;

    @Mock
    private ExpenseWorkflowGateway gateway;

    @Test
    void completedWorkflowRepairsApprovingExpense() throws Exception {
        ExpenseWorkflowRepository.BindingRow binding = new ExpenseWorkflowRepository.BindingRow(
                "b1", "tenant", ExpenseWorkflowService.BUSINESS_TYPE, "exp1", "wf1",
                "expense-reimbursement", "RUNNING", "APPROVING", "r1", "cb", "user1");
        when(repository.findReconciliationCandidates(100)).thenReturn(List.of(binding));
        when(gateway.getBusinessInstance("tenant", "exp1"))
                .thenReturn(new ObjectMapper().readTree(
                        "{\"instanceId\":\"wf1\",\"status\":\"COMPLETED\"}"));

        ExpenseWorkflowContracts.ReconciliationResponse response =
                new ExpenseWorkflowReconciliationService(repository, gateway).reconcile(100);

        assertEquals(1, response.repaired());
        verify(repository).reconcileStatus(
                "tenant", "exp1", "wf1", "COMPLETED", "APPROVED", true);
    }
}
