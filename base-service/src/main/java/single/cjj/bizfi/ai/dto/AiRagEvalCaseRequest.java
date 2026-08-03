package single.cjj.bizfi.ai.dto;

import java.util.List;

public record AiRagEvalCaseRequest(
        String question,
        List<String> expectedDocIds,
        List<String> expectedChunkIds,
        Integer topK,
        String status
) {
}
