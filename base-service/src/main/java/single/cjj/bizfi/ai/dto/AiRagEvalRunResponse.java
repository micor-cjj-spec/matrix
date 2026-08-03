package single.cjj.bizfi.ai.dto;

import java.time.LocalDateTime;

public record AiRagEvalRunResponse(
        String runId,
        String setId,
        String kbId,
        String status,
        int caseCount,
        int completedCount,
        int hitCount,
        Double hitAtK,
        Double mrr,
        Double recallAtK,
        Double averageLatencyMs,
        Long p95LatencyMs,
        String configSnapshot,
        String errorMessage,
        LocalDateTime startTime,
        LocalDateTime finishTime,
        LocalDateTime createTime,
        LocalDateTime modifyTime
) {
}
