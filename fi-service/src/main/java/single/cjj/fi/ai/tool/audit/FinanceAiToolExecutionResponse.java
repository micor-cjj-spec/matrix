package single.cjj.fi.ai.tool.audit;

public record FinanceAiToolExecutionResponse(
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

    public static FinanceAiToolExecutionResponse from(FinanceAiToolExecution entity) {
        return new FinanceAiToolExecutionResponse(
                entity.getFrequestid(),
                entity.getFconversationid(),
                entity.getFmodelname(),
                entity.getFmodeltraceid(),
                entity.getFtoolname(),
                entity.getFuserid(),
                entity.getForganizationid(),
                entity.getFperiod(),
                entity.getFstatus(),
                entity.getFreadinessscore(),
                entity.getFblockingcount(),
                entity.getFwarningcount(),
                entity.getFclosestatus(),
                entity.getFdurationms(),
                entity.getFerrorcode(),
                entity.getFerrormessage(),
                entity.getFstarttime() == null ? null : entity.getFstarttime().toString(),
                entity.getFendtime() == null ? null : entity.getFendtime().toString()
        );
    }
}
