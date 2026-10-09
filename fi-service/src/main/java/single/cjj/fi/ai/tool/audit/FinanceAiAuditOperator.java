package single.cjj.fi.ai.tool.audit;

import java.util.Set;

public record FinanceAiAuditOperator(
        String operatorId,
        Set<String> roles,
        String accessRequestId
) {
}
