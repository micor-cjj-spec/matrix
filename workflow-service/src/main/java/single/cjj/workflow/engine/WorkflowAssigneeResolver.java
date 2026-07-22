package single.cjj.workflow.engine;

import single.cjj.workflow.model.WorkflowDefinition;

import java.util.List;
import java.util.Map;

public interface WorkflowAssigneeResolver {

    boolean supports(WorkflowDefinition.AssigneeType type);

    Resolution resolve(WorkflowDefinition.AssigneeRule rule, Context context);

    record Context(String instanceId,
                   String initiatorId,
                   Map<String, Object> variables) {
    }

    record Candidate(String type, String value) {
    }

    record Resolution(List<Candidate> candidates) {
        public Resolution {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }
    }
}
