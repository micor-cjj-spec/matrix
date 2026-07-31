package single.cjj.fi.ai.tool.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import single.cjj.fi.ai.tool.FinanceAiToolProperties;

import java.util.UUID;

@Component
public class FinanceAiAuditStaleExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(FinanceAiAuditStaleExecutionScheduler.class);

    private final FinanceAiToolProperties properties;
    private final FinanceAiAuditOperatorGuard operatorGuard;
    private final FinanceAiAuditReconciliationCoordinator coordinator;

    public FinanceAiAuditStaleExecutionScheduler(
            FinanceAiToolProperties properties,
            FinanceAiAuditOperatorGuard operatorGuard,
            FinanceAiAuditReconciliationCoordinator coordinator
    ) {
        this.properties = properties;
        this.operatorGuard = operatorGuard;
        this.coordinator = coordinator;
    }

    @Scheduled(
            initialDelayString = "${matrix.ai-tool.audit-reconciliation-delay-ms:300000}",
            fixedDelayString = "${matrix.ai-tool.audit-reconciliation-delay-ms:300000}"
    )
    public void reconcileStaleExecutions() {
        if (!Boolean.TRUE.equals(properties.getAuditReconciliationEnabled())) {
            return;
        }
        String accessRequestId = "audit_system_" + UUID.randomUUID().toString().replace("-", "");
        try {
            FinanceAiAuditReconciliationResponse result = coordinator.reconcile(
                    operatorGuard.systemReconciler(accessRequestId)
            );
            if (result.timedOutCount() > 0) {
                log.warn(
                        "Reconciled stale AI tool executions, scanned={}, timedOut={}, cutoff={}",
                        result.scannedCount(),
                        result.timedOutCount(),
                        result.cutoff()
                );
            }
        } catch (RuntimeException failure) {
            log.error("Failed to reconcile stale AI tool executions", failure);
        }
    }
}
