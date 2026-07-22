package single.cjj.workflow.engine;

import single.cjj.workflow.model.WorkflowDefinition;

import java.util.Map;

public interface WorkflowNodeHandler {

    String handlerKey();

    ExecutionResult execute(ExecutionContext context);

    record ExecutionContext(
            String instanceId,
            WorkflowDefinition.Node node,
            Map<String, Object> variables
    ) {
    }

    record ExecutionResult(
            boolean success,
            String message,
            Map<String, Object> output
    ) {
        public static ExecutionResult success(Map<String, Object> output) {
            return new ExecutionResult(true, "OK", output == null ? Map.of() : output);
        }

        public static ExecutionResult failure(String message) {
            return new ExecutionResult(false, message, Map.of());
        }
    }
}
