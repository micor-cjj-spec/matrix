package single.cjj.fi.expense.workflow;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ExpenseWorkflowContracts {

    private ExpenseWorkflowContracts() {
    }

    public record CreateExpenseRequest(
            @NotBlank String tenantId,
            @NotBlank String applicantId,
            @NotBlank String departmentCode,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String currency,
            @NotBlank String description
    ) {
        public String safeCurrency() {
            return currency == null || currency.isBlank() ? "CNY" : currency.trim().toUpperCase();
        }
    }

    public record SubmitExpenseRequest(
            @NotBlank String operatorId,
            String definitionKey,
            String comment
    ) {
        public String safeDefinitionKey() {
            return definitionKey == null || definitionKey.isBlank()
                    ? "expense-reimbursement" : definitionKey.trim();
        }
    }

    public record ExpenseResponse(
            String id,
            String tenantId,
            String documentNumber,
            String applicantId,
            String departmentCode,
            BigDecimal amount,
            String currency,
            String description,
            String status,
            String workflowInstanceId,
            int version,
            LocalDateTime createdAt,
            LocalDateTime submittedAt,
            LocalDateTime completedAt
    ) {
    }

    public record WorkflowEventRequest(
            @NotBlank String eventId,
            @NotBlank String eventType,
            @NotBlank String instanceId,
            @NotBlank String tenantId,
            @NotBlank String sourceSystem,
            @NotBlank String businessType,
            @NotBlank String businessId,
            String status,
            Map<String, Object> variables,
            String occurredAt
    ) {
        public Map<String, Object> safeVariables() {
            return variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variables);
        }
    }

    public record WorkflowStartPayload(
            String tenantId,
            String definitionKey,
            String sourceSystem,
            String businessType,
            String businessId,
            String initiatorId,
            Map<String, Object> variables,
            String callbackUrl
    ) {
    }

    public record WorkflowResubmitPayload(
            String operatorId,
            String comment,
            Map<String, Object> variables
    ) {
    }
}
