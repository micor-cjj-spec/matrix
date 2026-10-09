package single.cjj.workflow.engine;

import org.junit.jupiter.api.Test;
import single.cjj.workflow.model.WorkflowDefinition;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowConditionEvaluatorCompositeTest {

    private final WorkflowConditionEvaluator evaluator = new WorkflowConditionEvaluator();

    @Test
    void evaluatesNestedAllAndAnyConditions() {
        WorkflowDefinition.Condition amount = leaf(
                "amount", WorkflowDefinition.ConditionOperator.GE, 1000);
        WorkflowDefinition.Condition region = leaf(
                "applicant.region", WorkflowDefinition.ConditionOperator.EQ, "WEST");
        WorkflowDefinition.Condition urgent = leaf(
                "urgent", WorkflowDefinition.ConditionOperator.EQ, true);

        WorkflowDefinition.Condition any = new WorkflowDefinition.Condition();
        any.setLogic(WorkflowDefinition.ConditionLogic.ANY);
        any.setChildren(List.of(region, urgent));

        WorkflowDefinition.Condition root = new WorkflowDefinition.Condition();
        root.setLogic(WorkflowDefinition.ConditionLogic.ALL);
        root.setChildren(List.of(amount, any));

        assertTrue(evaluator.matches(root, Map.of(
                "amount", 1200,
                "urgent", false,
                "applicant", Map.of("region", "WEST")
        )));
        assertFalse(evaluator.matches(root, Map.of(
                "amount", 1200,
                "urgent", false,
                "applicant", Map.of("region", "EAST")
        )));
    }

    private WorkflowDefinition.Condition leaf(String field,
                                              WorkflowDefinition.ConditionOperator operator,
                                              Object value) {
        WorkflowDefinition.Condition condition = new WorkflowDefinition.Condition();
        condition.setField(field);
        condition.setOperator(operator);
        condition.setValue(value);
        return condition;
    }
}
