package single.cjj.fi.ai.tool.audit;

import java.util.List;

public record FinanceAiToolExecutionPageResponse(
        int page,
        int size,
        long total,
        long totalPages,
        List<FinanceAiToolExecutionResponse> items
) {
    public static FinanceAiToolExecutionPageResponse of(
            int page,
            int size,
            long total,
            List<FinanceAiToolExecution> executions
    ) {
        long totalPages = total == 0 ? 0 : (total + size - 1) / size;
        List<FinanceAiToolExecutionResponse> items = executions == null
                ? List.of()
                : executions.stream().map(FinanceAiToolExecutionResponse::from).toList();
        return new FinanceAiToolExecutionPageResponse(page, size, total, totalPages, items);
    }
}
