package single.cjj.botp.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BotpContracts {

    private BotpContracts() {
    }

    public enum RuleStatus {
        DRAFT,
        TESTING,
        PUBLISHED,
        DISABLED,
        ARCHIVED
    }

    public enum MappingSourceType {
        SOURCE_FIELD,
        CONSTANT,
        CONTEXT
    }

    public enum ExecutionMode {
        SYNC,
        ASYNC
    }

    public enum ExecutionStatus {
        CREATED,
        VALIDATING,
        SOURCE_LOADING,
        TRANSFORMING,
        TARGET_CREATING,
        TARGET_CREATED,
        RELATION_SAVING,
        RELATION_SAVED,
        WRITEBACK_PENDING,
        WRITEBACK_PROCESSING,
        SUCCEEDED,
        FAILED,
        REVERSE_PENDING,
        REVERSING,
        REVERSED
    }

    public enum RelationStatus {
        PENDING,
        ACTIVE,
        INVALID,
        REVERSING,
        REVERSED
    }

    public enum WritebackTaskType {
        FORWARD_WRITEBACK,
        REVERSE_WRITEBACK,
        RECOMPUTE_WRITEBACK
    }

    public enum TaskStatus {
        PENDING,
        PROCESSING,
        SUCCEEDED,
        FAILED,
        DEAD
    }

    public enum ReconciliationIssueType {
        SOURCE_AMOUNT_MISMATCH,
        TARGET_VOID_RELATION_ACTIVE,
        EXECUTION_TARGET_WITHOUT_RELATION,
        TARGET_EXISTS_EXECUTION_INCOMPLETE
    }

    public enum ReconciliationStatus {
        OPEN,
        PROCESSING,
        FIXED,
        IGNORED
    }

    public record FieldMapping(
            MappingSourceType sourceType,
            String sourcePath,
            String targetPath,
            Object constantValue,
            boolean required
    ) {
    }

    public record WritebackMapping(
            String targetPath,
            String sourcePath,
            String mode
    ) {
    }

    public record RuleDefinition(
            String ruleCode,
            String ruleName,
            int version,
            RuleStatus status,
            String sourceSystemCode,
            String sourceDocumentType,
            String targetSystemCode,
            String targetDocumentType,
            List<FieldMapping> headerMappings,
            List<FieldMapping> entryMappings,
            List<WritebackMapping> writebackMappings
    ) {
        public RuleDefinition {
            headerMappings = immutable(headerMappings);
            entryMappings = immutable(entryMappings);
            writebackMappings = immutable(writebackMappings);
        }
    }

    public record DocumentRef(
            @NotBlank String systemCode,
            @NotBlank String documentType,
            @NotBlank String documentId,
            List<String> entryIds
    ) {
        public DocumentRef {
            entryIds = immutable(entryIds);
        }
    }

    public record DocumentData(
            DocumentRef reference,
            Map<String, Object> header,
            List<Map<String, Object>> entries
    ) {
        public DocumentData {
            header = immutableMap(header);
            entries = immutableMaps(entries);
        }
    }

    public record TargetDraft(
            String systemCode,
            String documentType,
            Map<String, Object> header,
            List<Map<String, Object>> entries
    ) {
        public TargetDraft {
            header = immutableMap(header);
            entries = immutableMaps(entries);
        }
    }

    public record TargetResult(
            String systemCode,
            String documentType,
            String documentId,
            String documentNo
    ) {
    }

    public record ExecutionRequest(
            @NotBlank String requestId,
            @NotBlank String sourceSystem,
            @NotBlank String tenantId,
            @NotBlank String ruleCode,
            @NotEmpty List<@Valid DocumentRef> sourceDocuments,
            Map<String, Object> parameters,
            ExecutionMode executionMode,
            String callbackUrl
    ) {
        public ExecutionRequest {
            sourceDocuments = immutable(sourceDocuments);
            parameters = immutableMap(parameters);
            executionMode = executionMode == null ? ExecutionMode.SYNC : executionMode;
        }
    }

    public record PreviewResult(
            String ruleCode,
            int ruleVersion,
            List<TargetDraft> targetDrafts,
            List<String> warnings
    ) {
        public PreviewResult {
            targetDrafts = immutable(targetDrafts);
            warnings = immutable(warnings);
        }
    }

    public record ExecutionResult(
            String executionId,
            String ruleCode,
            int ruleVersion,
            ExecutionStatus status,
            List<TargetResult> targetDocuments,
            String errorMessage
    ) {
        public ExecutionResult {
            targetDocuments = immutable(targetDocuments);
        }
    }

    public record ExecutionDetails(
            String tenantId,
            String sourceSystem,
            String requestId,
            String executionId,
            String ruleCode,
            int ruleVersion,
            ExecutionMode executionMode,
            ExecutionStatus status,
            List<DocumentRef> sourceDocuments,
            List<TargetResult> targetDocuments,
            String errorMessage,
            LocalDateTime startTime,
            LocalDateTime finishTime
    ) {
        public ExecutionDetails {
            sourceDocuments = immutable(sourceDocuments);
            targetDocuments = immutable(targetDocuments);
        }

        public ExecutionResult toResult() {
            return new ExecutionResult(executionId, ruleCode, ruleVersion, status, targetDocuments, errorMessage);
        }
    }

    public record DocumentRelation(
            Long relationId,
            String tenantId,
            String executionId,
            String ruleCode,
            int ruleVersion,
            DocumentRef sourceDocument,
            TargetResult targetDocument,
            BigDecimal allocatedAmount,
            RelationStatus status,
            String targetStatus,
            String lastEventId,
            String invalidReason,
            LocalDateTime createdTime,
            LocalDateTime invalidTime,
            LocalDateTime reversedTime
    ) {
    }

    public record ExecutionLog(
            Long logId,
            String executionId,
            String stage,
            TaskStatus status,
            String message,
            String requestSnapshot,
            String responseSnapshot,
            String exceptionType,
            LocalDateTime startTime,
            LocalDateTime finishTime
    ) {
    }

    public record WritebackTask(
            Long taskId,
            String tenantId,
            String executionId,
            Long relationId,
            DocumentRef sourceDocument,
            TargetResult targetDocument,
            WritebackTaskType taskType,
            TaskStatus status,
            BigDecimal activeAllocatedAmount,
            BigDecimal releaseReservedAmount,
            int retryCount,
            LocalDateTime nextRetryTime,
            String errorMessage,
            LocalDateTime createdTime,
            LocalDateTime finishTime
    ) {
    }

    public record ReconciliationIssue(
            Long issueId,
            String tenantId,
            ReconciliationIssueType issueType,
            ReconciliationStatus status,
            String executionId,
            Long relationId,
            DocumentRef sourceDocument,
            TargetResult targetDocument,
            BigDecimal expectedAmount,
            BigDecimal actualAmount,
            String description,
            String resolution,
            LocalDateTime detectedTime,
            LocalDateTime resolvedTime
    ) {
    }

    public record TargetStatusEvent(
            @NotBlank String eventId,
            @NotBlank String tenantId,
            @NotBlank String targetSystemCode,
            @NotBlank String targetDocumentType,
            @NotBlank String targetDocumentId,
            @NotBlank String targetStatus,
            String reason,
            String operator,
            LocalDateTime eventTime
    ) {
        public TargetStatusEvent {
            eventTime = eventTime == null ? LocalDateTime.now() : eventTime;
        }
    }

    public record RelationInvalidateRequest(
            @NotBlank String eventId,
            @NotBlank String reason,
            String operator
    ) {
    }

    public record ReconciliationActionRequest(
            String resolution,
            String operator
    ) {
    }

    public record WritebackCommand(
            String executionId,
            DocumentRef sourceDocument,
            TargetResult targetDocument,
            List<WritebackMapping> mappings,
            Map<String, Object> context
    ) {
        public WritebackCommand {
            mappings = immutable(mappings);
            context = immutableMap(context);
        }

        public WritebackCommand(
                String executionId,
                DocumentRef sourceDocument,
                TargetResult targetDocument,
                List<WritebackMapping> mappings
        ) {
            this(executionId, sourceDocument, targetDocument, mappings, Map.of());
        }
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<Map<String, Object>> immutableMaps(List<Map<String, Object>> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(BotpContracts::immutableMap).toList();
    }

    private static Map<String, Object> immutableMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
