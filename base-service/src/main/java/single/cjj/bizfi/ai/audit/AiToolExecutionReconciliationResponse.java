package single.cjj.bizfi.ai.audit;

public record AiToolExecutionReconciliationResponse(
        String cutoff,
        Integer scannedCount,
        Integer timedOutCount
) {
}
