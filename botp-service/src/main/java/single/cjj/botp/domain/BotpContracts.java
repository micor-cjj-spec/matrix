package single.cjj.botp.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

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
        SOURCE_LOADING,
        TRANSFORMING,
        TARGET_CREATING,
        TARGET_CREATED,
        RELATION_SAVED,
        WRITEBACK_PENDING,
        SUCCEEDED,
        FAILED
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

    public record WritebackCommand(
            String executionId,
            DocumentRef sourceDocument,
            TargetResult targetDocument,
            List<WritebackMapping> mappings
    ) {
        public WritebackCommand {
            mappings = immutable(mappings);
        }
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<Map<String, Object>> immutableMaps(List<Map<String, Object>> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(BotpContracts::immutableMap)
                .toList();
    }

    private static Map<String, Object> immutableMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
