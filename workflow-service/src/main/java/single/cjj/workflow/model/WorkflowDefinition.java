package single.cjj.workflow.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class WorkflowDefinition {

    private List<Node> nodes = new ArrayList<>();
    private List<Transition> transitions = new ArrayList<>();

    public Node requireNode(String nodeKey) {
        return nodes.stream()
                .filter(node -> nodeKey.equals(node.getKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("流程节点不存在: " + nodeKey));
    }

    public Node requireStartNode() {
        return nodes.stream()
                .filter(node -> node.getType() == NodeType.START)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("流程定义缺少 START 节点"));
    }

    @Data
    @NoArgsConstructor
    public static class Node {
        private String key;
        private String name;
        private NodeType type;
        private String handlerKey;
        private AssigneeRule assigneeRule;
        private Map<String, Object> config = new LinkedHashMap<>();
    }

    @Data
    @NoArgsConstructor
    public static class Transition {
        private String from;
        private String to;
        private Condition condition;
        private Integer priority = 0;
    }

    @Data
    @NoArgsConstructor
    public static class Condition {
        private String field;
        private ConditionOperator operator;
        private Object value;
    }

    @Data
    @NoArgsConstructor
    public static class AssigneeRule {
        private AssigneeType type;
        private String value;
    }

    public enum NodeType {
        START,
        USER_TASK,
        SERVICE_TASK,
        EXCLUSIVE_GATEWAY,
        END
    }

    public enum ConditionOperator {
        EQ,
        NE,
        GT,
        GE,
        LT,
        LE,
        IN,
        NOT_IN,
        IS_NULL,
        NOT_NULL
    }

    public enum AssigneeType {
        USER,
        ROLE,
        VARIABLE
    }
}
