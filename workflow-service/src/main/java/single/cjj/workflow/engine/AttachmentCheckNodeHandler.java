package single.cjj.workflow.engine;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
public class AttachmentCheckNodeHandler implements WorkflowNodeHandler {

    public static final String HANDLER_KEY = "attachment-check";

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

        Set<String> required = toStringSet(requiredCategories);
        Object uploadedValue = context.variables().get("attachmentCategories");
        Set<String> uploaded = uploadedValue instanceof Collection<?> collection
                ? toStringSet(collection)
                : Set.of();

        Set<String> missing = new LinkedHashSet<>(required);
        missing.removeAll(uploaded);
        if (!missing.isEmpty()) {
            return ExecutionResult.failure("缺少必要影像分类: " + String.join(",", missing));
        }
        return ExecutionResult.success(Map.of("checked", true, "categories", uploaded));
    }

    private Set<String> toStringSet(Collection<?> values) {
        Set<String> result = new LinkedHashSet<>();
        for (Object value : values) {
            if (value != null) {
                result.add(String.valueOf(value));
            }
        }
        return result;
    }
}
