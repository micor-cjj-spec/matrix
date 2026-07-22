package single.cjj.fi.expense.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ExpenseWorkflowReconciliationService {

    private final ExpenseWorkflowRepository repository;
    private final ExpenseWorkflowGateway gateway;

    public ExpenseWorkflowReconciliationService(ExpenseWorkflowRepository repository,
                                                ExpenseWorkflowGateway gateway) {
        this.repository = repository;
        this.gateway = gateway;
    }

    @Scheduled(fixedDelayString = "${fi.workflow.reconciliation.delay-ms:600000}")
    public void scheduledReconcile() {
        reconcile(100);
    }

    public ExpenseWorkflowContracts.ReconciliationResponse reconcile(int requestedLimit) {
        int limit = Math.min(Math.max(requestedLimit, 1), 500);
        List<ExpenseWorkflowRepository.BindingRow> rows = repository.findReconciliationCandidates(limit);
        int repaired = 0;
        int failed = 0;
        for (ExpenseWorkflowRepository.BindingRow binding : rows) {
            try {
                JsonNode workflow = gateway.getBusinessInstance(binding.tenantId(), binding.businessId());
                String workflowStatus = workflow.path("status").asText();
                String businessStatus = mapBusinessStatus(workflowStatus);
                if (businessStatus == null) {
                    continue;
                }
                if (!workflowStatus.equals(binding.workflowStatus())
                        || !businessStatus.equals(binding.businessStatus())) {
                    repository.reconcileStatus(binding.tenantId(), binding.businessId(),
                            workflow.path("instanceId").asText(binding.workflowInstanceId()),
                            workflowStatus, businessStatus, isTerminal(workflowStatus));
                    repository.upsertReconciliationIssue(new ExpenseWorkflowRepository.ReconciliationIssueRow(
                            newId(), binding.tenantId(), binding.businessType(), binding.businessId(),
                            binding.workflowInstanceId(), "STATUS_MISMATCH", binding.businessStatus(),
                            binding.workflowStatus(), businessStatus, workflowStatus,
                            "AUTO_RESOLVED", null, LocalDateTime.now()));
                    repaired++;
                }
            } catch (Exception ex) {
                failed++;
                repository.upsertReconciliationIssue(new ExpenseWorkflowRepository.ReconciliationIssueRow(
                        newId(), binding.tenantId(), binding.businessType(), binding.businessId(),
                        binding.workflowInstanceId(), "QUERY_FAILED", binding.businessStatus(),
                        binding.workflowStatus(), null, null, "OPEN", ex.getMessage(), LocalDateTime.now()));
                log.warn("expense workflow reconciliation failed, businessId={}", binding.businessId(), ex);
            }
        }
        return new ExpenseWorkflowContracts.ReconciliationResponse(rows.size(), repaired, failed);
    }

    private String mapBusinessStatus(String workflowStatus) {
        return switch (workflowStatus) {
            case "RUNNING" -> "APPROVING";
            case "WAITING_RESUBMIT" -> "RETURNED";
            case "COMPLETED" -> "APPROVED";
            case "REJECTED" -> "REJECTED";
            case "CANCELLED" -> "CANCELLED";
            default -> null;
        };
    }

    private boolean isTerminal(String workflowStatus) {
        return "COMPLETED".equals(workflowStatus)
                || "REJECTED".equals(workflowStatus)
                || "CANCELLED".equals(workflowStatus);
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
