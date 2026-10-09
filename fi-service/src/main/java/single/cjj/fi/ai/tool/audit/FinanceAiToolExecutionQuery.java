package single.cjj.fi.ai.tool.audit;

import java.time.LocalDateTime;

public record FinanceAiToolExecutionQuery(
        Long userId,
        Long organizationId,
        String period,
        String status,
        String conversationId,
        String modelTraceId,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        int page,
        int size
) {
}
