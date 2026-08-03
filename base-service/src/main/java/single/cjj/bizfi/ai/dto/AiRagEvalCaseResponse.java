package single.cjj.bizfi.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiRagEvalCaseResponse(
        String caseId,
        String setId,
        String question,
        List<String> expectedDocIds,
        List<String> expectedChunkIds,
        int topK,
        String status,
        LocalDateTime createTime,
        LocalDateTime modifyTime
) {
}
