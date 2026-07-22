package single.cjj.workflow.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import single.cjj.workflow.model.WorkflowDefinition;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WorkflowContracts {

    private WorkflowContracts() {
    }

    public record DefinitionCreateRequest(
            @NotBlank String tenantId,
            @NotBlank String definitionKey,
            @NotBlank String definitionName,
            @NotNull @Valid WorkflowDefinition definition,
            @NotBlank String createdBy
    ) {
    }

    public record DefinitionResponse(
            String tenantId,
            String definitionKey,
            String definitionName,
            int version,
            String status,
            WorkflowDefinition definition,
            LocalDateTime createdAt,
            LocalDateTime publishedAt
    ) {
    }

    public record StartWorkflowRequest(
            @NotBlank String tenantId,
            @NotBlank String definitionKey,
            @NotBlank String sourceSystem,
            @NotBlank String businessType,
            @NotBlank String businessId,
            @NotBlank String initiatorId,
            Map<String, Object> variables,
            String callbackUrl
    ) {
        public Map<String, Object> safeVariables() {
            return variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variables);
        }
    }

    public record TaskActionRequest(
            @NotNull TaskAction action,
            @NotBlank String operatorId,
            String comment,
            Map<String, Object> variables
    ) {
        public Map<String, Object> safeVariables() {
            return variables == null ? Map.of() : variables;
        }
    }

    public record ResubmitInstanceRequest(
            @NotBlank String operatorId,
            String comment,
            Map<String, Object> variables
    ) {
        public Map<String, Object> safeVariables() {
            return variables == null ? Map.of() : variables;
        }
    }

    public record CancelInstanceRequest(
            @NotBlank String operatorId,
            String reason
    ) {
    }

    public enum TaskAction {
        APPROVE,
        REJECT,
        RETURN_TO_INITIATOR
    }

    public record InstanceResponse(
            String instanceId,
            String tenantId,
            String definitionKey,
            int definitionVersion,
            String sourceSystem,
            String businessType,
            String businessId,
            String initiatorId,
            String currentNodeKey,
            String status,
            int version,
            Map<String, Object> variables,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
    }

    public record TaskResponse(
            String taskId,
            String instanceId,
            String nodeInstanceId,
            String nodeKey,
            String taskName,
            String assigneeType,
            String assigneeValue,
            String status,
            int version,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
    }
}
