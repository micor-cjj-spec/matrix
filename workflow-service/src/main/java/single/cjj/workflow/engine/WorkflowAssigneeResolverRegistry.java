package single.cjj.workflow.engine;

import org.springframework.stereotype.Component;
import single.cjj.bizfi.exception.BizException;
import single.cjj.workflow.model.WorkflowDefinition;

import java.util.List;

@Component
public class WorkflowAssigneeResolverRegistry {

    private final List<WorkflowAssigneeResolver> resolvers;

    public WorkflowAssigneeResolverRegistry(List<WorkflowAssigneeResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    public WorkflowAssigneeResolver.Resolution resolve(WorkflowDefinition.AssigneeRule rule,
                                                       WorkflowAssigneeResolver.Context context) {
        if (rule == null || rule.getType() == null) {
            throw new BizException("人工节点未配置审批人规则");
        }
        return resolvers.stream()
                .filter(resolver -> resolver.supports(rule.getType()))
                .findFirst()
                .orElseThrow(() -> new BizException("没有可用的审批人解析器: " + rule.getType()))
                .resolve(rule, context);
    }

    public static WorkflowAssigneeResolverRegistry defaultRegistry() {
        return new WorkflowAssigneeResolverRegistry(List.of(new DefaultWorkflowAssigneeResolver()));
    }
}
