package single.cjj.bizfi.ai.dto;

public record AiRagEvalConfigResponse(
        boolean enabled,
        int maxCasesPerSet,
        long pollDelayMs,
        String migration
) {
}
