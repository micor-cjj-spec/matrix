package single.cjj.workflow.engine;

import org.junit.jupiter.api.Test;
import single.cjj.workflow.model.WorkflowDefinition;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowConditionEvaluatorTest {

    private final WorkflowConditionEvaluator evaluator = new WorkflowConditionEvaluator();

    @Test
    void shouldRouteByAmountAndUseDefaultTransition() {
        WorkflowDefinition definition = new WorkflowDefinition();

        WorkflowDefinition.Transition highAmount = new WorkflowDefinition.Transition();
        highAmount.setFrom("amountGateway");
        highAmount.setTo("secondReview");
        highAmount.setPriority(10);
        WorkflowDefinition.Condition condition = new WorkflowDefinition.Condition();
        condition.setField("amount");
        condition.setOperator(WorkflowDefinition.ConditionOperator.GT);
        condition.setValue(10000);
        highAmount.setCondition(condition);

        WorkflowDefinition.Transition defaultRoute = new WorkflowDefinition.Transition();
        defaultRoute.setFrom("amountGateway");
        defaultRoute.setTo("end");
        defaultRoute.setPriority(0);

        definition.setTransitions(List.of(highAmount, defaultRoute));

        assertEquals("secondReview", evaluator.resolveNextNode(
                definition, "amountGateway", Map.of("amount", 12000)
        ));
        assertEquals("end", evaluator.resolveNextNode(
                definition, "amountGateway", Map.of("amount", 5000)
        ));
    }

    @Test
    void shouldCompareNumericValuesWithoutTypeMismatch() {
        WorkflowDefinition.Condition condition = new WorkflowDefinition.Condition();
        condition.setField("amount");
        condition.setOperator(WorkflowDefinition.ConditionOperator.EQ);
        condition.setValue(10000L);

        assertTrue(evaluator.matches(condition, Map.of("amount", 10000)));
    }
}
