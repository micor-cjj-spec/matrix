package single.cjj.fi.ai.tool.audit;

import single.cjj.fi.ai.tool.FinanceMonthEndCloseToolRequest;
import single.cjj.fi.ai.tool.FinanceMonthEndCloseToolResponse;

import java.util.Optional;

public interface FinanceAiToolAuditService {

    void recordStarted(String toolName, FinanceMonthEndCloseToolRequest request);

    void recordSucceeded(
            String toolName,
            FinanceMonthEndCloseToolRequest request,
            FinanceMonthEndCloseToolResponse response,
            long durationMillis
    );

    void recordFailed(
            String toolName,
            FinanceMonthEndCloseToolRequest request,
            Throwable failure,
            long durationMillis
    );

    Optional<FinanceAiToolExecution> findByRequestId(String requestId);
}
