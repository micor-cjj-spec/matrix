package single.cjj.fi.ai.tool.audit;

import org.springframework.stereotype.Service;
import single.cjj.fi.ai.tool.FinanceAiToolProperties;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class FinanceAiAuditReconciliationCoordinator {

    private final FinanceAiToolProperties properties;
    private final FinanceAiToolAuditService auditService;
    private final FinanceAiAuditAccessLogService accessLogService;

    public FinanceAiAuditReconciliationCoordinator(
            FinanceAiToolProperties properties,
            FinanceAiToolAuditService auditService,
            FinanceAiAuditAccessLogService accessLogService
    ) {
        this.properties = properties;
        this.auditService = auditService;
        this.accessLogService = accessLogService;
    }

    public FinanceAiAuditReconciliationResponse reconcile(FinanceAiAuditOperator operator) {
        long startedAt = System.nanoTime();
        LocalDateTime reconciledAt = LocalDateTime.now();
        int timeoutMinutes = normalizePositive(properties.getAuditStartedTimeoutMinutes(), 15, 10080);
        int batchSize = normalizePositive(properties.getAuditReconciliationBatchSize(), 100, 1000);
        LocalDateTime cutoff = reconciledAt.minusMinutes(timeoutMinutes);
        String summary = "cutoff=" + cutoff + ";batchSize=" + batchSize;
        try {
            FinanceAiToolReconciliationResult result = auditService.reconcileStaleStarted(
                    cutoff,
                    batchSize,
                    reconciledAt
            );
            accessLogService.recordRequired(
                    operator,
                    FinanceAiAuditAccessLogService.RECONCILE,
                    summary,
                    FinanceAiAuditAccessLogService.SUCCESS,
                    result.timedOutCount(),
                    elapsedMillis(startedAt),
                    null
            );
            return FinanceAiAuditReconciliationResponse.from(result);
        } catch (RuntimeException failure) {
            accessLogService.recordRequired(
                    operator,
                    FinanceAiAuditAccessLogService.RECONCILE,
                    summary,
                    FinanceAiAuditAccessLogService.FAILED,
                    0,
                    elapsedMillis(startedAt),
                    failure
            );
            throw failure;
        }
    }

    private int normalizePositive(Integer value, int fallback, int maximum) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return Math.min(value, maximum);
    }

    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - startedAtNanos));
    }
}
