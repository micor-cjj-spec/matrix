package single.cjj.fi.ai.tool.audit;

public record FinanceAiAuditReconciliationResponse(
        String cutoff,
        Integer scannedCount,
        Integer timedOutCount
) {

    public static FinanceAiAuditReconciliationResponse from(FinanceAiToolReconciliationResult result) {
        return new FinanceAiAuditReconciliationResponse(
                result.cutoff().toString(),
                result.scannedCount(),
                result.timedOutCount()
        );
    }
}
