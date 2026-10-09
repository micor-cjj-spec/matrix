package single.cjj.fi.ai.tool.audit;

import java.time.LocalDateTime;

public record FinanceAiToolReconciliationResult(
        LocalDateTime cutoff,
        int scannedCount,
        int timedOutCount
) {
}
