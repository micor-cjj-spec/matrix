package single.cjj.workflow.engine;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowNodeHandlerRegistry {

    private final Map<String, WorkflowNodeHandler> handlers = new HashMap<>();

    public WorkflowNodeHandlerRegistry(List<WorkflowNodeHandler> handlerList) {
        for (WorkflowNodeHandler handler : handlerList) {
            WorkflowNodeHandler previous = handlers.put(handler.handlerKey(), handler);
            if (previous != null) {
                throw new IllegalStateException("重复的工作流 Handler: " + handler.handlerKey());
            }
        }
    }

    public WorkflowNodeHandler require(String handlerKey) {
        WorkflowNodeHandler handler = handlers.get(handlerKey);
        if (handler == null) {
            throw new IllegalStateException("工作流 Handler 未注册: " + handlerKey);
        }
        return handler;
    }
}
