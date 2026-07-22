package single.cjj.workflow.engine;

import org.springframework.stereotype.Component;
import single.cjj.workflow.attachment.WorkflowAttachmentRepository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class AttachmentCheckNodeHandler implements WorkflowNodeHandler {

    public static final String HANDLER_KEY = "attachment-check";

    private final WorkflowAttachmentRepository attachmentRepository;

    public AttachmentCheckNodeHandler(WorkflowAttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    public String handlerKey() {
        return HANDLER_KEY;
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        Object requiredValue = context.node().getConfig().get("requiredCategories");
        if (!(requiredValue instanceof Collection<?> requiredCategories) || requiredCategories.isEmpty()) {
            return ExecutionResult.success(Map.of("checked", true));
        }

        Map<String, Long> uploaded = new LinkedHashMap<>(
                attachmentRepository.countUploadedCategories(context.instanceId()));
        mergeLegacyVariables(context, uploaded);

        Map<String, Long> missing = new LinkedHashMap<>();
        for (Object item : requiredCategories) {
            Requirement requirement = parseRequirement(item);
            long actual = uploaded.getOrDefault(requirement.category(), 0L);
            if (actual < requirement.minimumCount()) {
                missing.put(requirement.category(), requirement.minimumCount() - actual);
            }
        }
        if (!missing.isEmpty()) {
            String message = missing.entrySet().stream()
                    .map(entry -> entry.getKey() + "缺少" + entry.getValue() + "份")
                    .reduce((left, right) -> left + "," + right)
                    .orElse("缺少必要影像");
            return ExecutionResult.failure("缺少必要影像分类: " + message);
        }
        return ExecutionResult.success(Map.of("checked", true, "categoryCounts", uploaded));
    }

    private Requirement parseRequirement(Object item) {
        if (item instanceof Map<?, ?> map) {
            Object categoryValue = map.get("category");
            if (categoryValue == null) {
                categoryValue = map.get("categoryCode");
            }
            String category = normalize(categoryValue);
            Object minimumValue = map.get("minimumCount");
            long minimum = minimumValue instanceof Number number
                    ? Math.max(1L, number.longValue()) : 1L;
            return new Requirement(category, minimum);
        }
        return new Requirement(normalize(item), 1L);
    }

    private void mergeLegacyVariables(ExecutionContext context, Map<String, Long> uploaded) {
        Object value = context.variables().get("attachmentCategories");
        if (value instanceof Collection<?> categories) {
            for (Object category : categories) {
                uploaded.merge(normalize(category), 1L, Long::sum);
            }
        }
    }

    private String normalize(Object value) {
        return String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private record Requirement(String category, long minimumCount) {
    }
}
