package single.cjj.workflow.engine;

import org.springframework.stereotype.Component;
import single.cjj.workflow.model.WorkflowDefinition;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class WorkflowConditionEvaluator {

    public String resolveNextNode(WorkflowDefinition definition,
                                  String currentNodeKey,
                                  Map<String, Object> variables) {
        List<WorkflowDefinition.Transition> outgoing = definition.getTransitions().stream()
                .filter(transition -> currentNodeKey.equals(transition.getFrom()))
                .sorted(Comparator.comparing(
                        WorkflowDefinition.Transition::getPriority,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        WorkflowDefinition.Transition defaultTransition = null;
        for (WorkflowDefinition.Transition transition : outgoing) {
            if (transition.getCondition() == null) {
                if (defaultTransition == null) {
                    defaultTransition = transition;
                }
                continue;
            }
            if (matches(transition.getCondition(), variables)) {
                return transition.getTo();
            }
        }
        if (defaultTransition != null) {
            return defaultTransition.getTo();
        }
        throw new IllegalStateException("节点没有可执行的后续连线: " + currentNodeKey);
    }

    public boolean matches(WorkflowDefinition.Condition condition,
                           Map<String, Object> variables) {
        if (condition == null) {
            return true;
        }
        List<WorkflowDefinition.Condition> children = condition.getChildren();
        if (children != null && !children.isEmpty()) {
            WorkflowDefinition.ConditionLogic logic = condition.getLogic() == null
                    ? WorkflowDefinition.ConditionLogic.ALL : condition.getLogic();
            return logic == WorkflowDefinition.ConditionLogic.ANY
                    ? children.stream().anyMatch(child -> matches(child, variables))
                    : children.stream().allMatch(child -> matches(child, variables));
        }

        Object actual = resolvePath(variables, condition.getField());
        Object expected = condition.getValue();
        WorkflowDefinition.ConditionOperator operator = condition.getOperator();
        if (operator == null) {
            throw new IllegalArgumentException("条件操作符不能为空");
        }

        return switch (operator) {
            case IS_NULL -> actual == null;
            case NOT_NULL -> actual != null;
            case EQ -> equalsValue(actual, expected);
            case NE -> !equalsValue(actual, expected);
            case GT -> compare(actual, expected) > 0;
            case GE -> compare(actual, expected) >= 0;
            case LT -> compare(actual, expected) < 0;
            case LE -> compare(actual, expected) <= 0;
            case IN -> expected instanceof Collection<?> collection
                    && collection.stream().anyMatch(item -> equalsValue(actual, item));
            case NOT_IN -> expected instanceof Collection<?> collection
                    && collection.stream().noneMatch(item -> equalsValue(actual, item));
        };
    }

    private Object resolvePath(Map<String, Object> variables, String path) {
        if (variables == null || path == null || path.isBlank()) {
            return null;
        }
        Object current = variables;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    private boolean equalsValue(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            return decimal(actual).compareTo(decimal(expected)) == 0;
        }
        return Objects.equals(actual, expected);
    }

    private int compare(Object actual, Object expected) {
        if (actual == null || expected == null) {
            throw new IllegalArgumentException("比较条件的值不能为空");
        }
        if (actual instanceof Number && expected instanceof Number) {
            return decimal(actual).compareTo(decimal(expected));
        }
        return String.valueOf(actual).compareTo(String.valueOf(expected));
    }

    private BigDecimal decimal(Object value) {
        return new BigDecimal(String.valueOf(value));
    }
}
