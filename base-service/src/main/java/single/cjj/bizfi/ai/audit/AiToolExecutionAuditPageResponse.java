package single.cjj.bizfi.ai.audit;

import java.util.List;

public record AiToolExecutionAuditPageResponse(
        Integer page,
        Integer size,
        Long total,
        Long totalPages,
        List<AiToolExecutionAuditResponse> items
) {
}
