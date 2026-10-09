package single.cjj.bizfi.ai.audit;

public record AiToolExecutionAuditResponse(
        String requestId,
        String conversationId,
        String modelName,
        String modelTraceId,
        String toolName,
        Long userId,
        Long organizationId,
        String period,
        String status,
        Integer readinessScore,
        Integer blockingCount,
        Integer warningCount,
        String closeStatus,
        Long durationMillis,
        String errorCode,
        String errorMessage,
        String startedAt,
        String endedAt
) {
}
