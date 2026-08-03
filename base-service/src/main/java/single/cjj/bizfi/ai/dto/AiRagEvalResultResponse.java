package single.cjj.bizfi.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiRagEvalResultResponse(
        String runId,
        String caseId,
        String question,
        List<String> expectedDocIds,
        List<String> expectedChunkIds,
        List<String> retrievedDocIds,
        List<String> retrievedChunkIds,
        boolean hit,
        Integer firstRelevantRank,
        double reciprocalRank,
        double recallAtK,
        long latencyMs,
        String errorMessage,
        LocalDateTime createTime
) {
}
