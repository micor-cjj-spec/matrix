package single.cjj.workflow.engine;

import org.junit.jupiter.api.Test;
import single.cjj.workflow.model.WorkflowDefinition;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultWorkflowAssigneeResolverTest {

    private final DefaultWorkflowAssigneeResolver resolver = new DefaultWorkflowAssigneeResolver();

    @Test
    void resolvesMultipleUsersFromNestedVariable() {
        WorkflowDefinition.AssigneeRule rule = new WorkflowDefinition.AssigneeRule();
        rule.setType(WorkflowDefinition.AssigneeType.USERS_VARIABLE);
        rule.setValue("approval.reviewers");

        WorkflowAssigneeResolver.Resolution resolution = resolver.resolve(
                rule,
                new WorkflowAssigneeResolver.Context(
                        "wf1",
                        "starter",
                        Map.of("approval", Map.of("reviewers", List.of("u1", "u2", "u1")))
                ));

        assertEquals(List.of(
                new WorkflowAssigneeResolver.Candidate("USER", "u1"),
                new WorkflowAssigneeResolver.Candidate("USER", "u2")
        ), resolution.candidates());
    }

    @Test
    void resolvesStaticRolesAndInitiator() {
        WorkflowDefinition.AssigneeRule roleRule = new WorkflowDefinition.AssigneeRule();
        roleRule.setType(WorkflowDefinition.AssigneeType.ROLES);
        roleRule.setValues(List.of("FINANCE_REVIEWER", "FINANCE_MANAGER"));
        WorkflowAssigneeResolver.Resolution roles = resolver.resolve(
                roleRule,
                new WorkflowAssigneeResolver.Context("wf1", "starter", Map.of()));
        assertEquals("ROLE", roles.candidates().get(0).type());
        assertEquals(2, roles.candidates().size());

        WorkflowDefinition.AssigneeRule initiatorRule = new WorkflowDefinition.AssigneeRule();
        initiatorRule.setType(WorkflowDefinition.AssigneeType.INITIATOR);
        WorkflowAssigneeResolver.Resolution initiator = resolver.resolve(
                initiatorRule,
                new WorkflowAssigneeResolver.Context("wf1", "starter", Map.of()));
        assertEquals(List.of(
                new WorkflowAssigneeResolver.Candidate("USER", "starter")
        ), initiator.candidates());
    }
}
