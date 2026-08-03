package single.cjj.bizfi.ai.dto;

import java.time.LocalDateTime;

public record AiRagEvalSetResponse(
        String setId,
        String kbId,
        String name,
        String description,
        String status,
        long caseCount,
        LocalDateTime createTime,
        LocalDateTime modifyTime
) {
}
