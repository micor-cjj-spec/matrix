package single.cjj.workflow.engine;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.workflow.model.WorkflowDefinition;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DefaultWorkflowAssigneeResolver implements WorkflowAssigneeResolver {

    @Override
    public boolean supports(WorkflowDefinition.AssigneeType type) {
        return type != null;
    }

    @Override
    public Resolution resolve(WorkflowDefinition.AssigneeRule rule, Context context) {
        if (rule == null || rule.getType() == null) {
            throw new BizException("人工节点未配置审批人规则");
        }
        List<Candidate> candidates = switch (rule.getType()) {
            case USER -> typed("USER", rule.getValue());
            case ROLE -> typed("ROLE", rule.getValue());
            case INITIATOR -> typed("USER", context.initiatorId());
            case USERS -> typed("USER", rule.getValues());
            case ROLES -> typed("ROLE", rule.getValues());
            case VARIABLE, USERS_VARIABLE -> typed(
                    "USER", resolveVariable(context.variables(), rule.getValue()));
            case ROLES_VARIABLE -> typed(
                    "ROLE", resolveVariable(context.variables(), rule.getValue()));
        };
        if (candidates.isEmpty()) {
            throw new BizException("审批人规则没有解析到任何候选人");
        }
        return new Resolution(candidates);
    }

    private List<Candidate> typed(String type, Object rawValue) {
        Set<String> values = new LinkedHashSet<>();
        flatten(rawValue, values);
        List<Candidate> candidates = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                candidates.add(new Candidate(type, value.trim()));
            }
        }
        return candidates;
    }

    private void flatten(Object value, Set<String> target) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> flatten(item, target));
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                flatten(Array.get(value, index), target);
            }
            return;
        }
        String text = String.valueOf(value);
        for (String part : text.split(",")) {
            if (StringUtils.hasText(part)) {
                target.add(part.trim());
            }
        }
    }

    private Object resolveVariable(Map<String, Object> variables, String path) {
        if (!StringUtils.hasText(path)) {
            throw new BizException("审批人变量名不能为空");
        }
        Object current = variables;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                current = null;
                break;
            }
            current = map.get(segment);
        }
        if (current == null) {
            throw new BizException("审批人变量不存在: " + path);
        }
        return current;
    }
}
