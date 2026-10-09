package single.cjj.bizfi.ai.dto;

import java.time.LocalDateTime;

public record AiKnowledgeIndexJobResponse(
        String jobId,
        String kbId,
        String docId,
        String fileName,
        String mediaType,
        long fileSize,
        String contentHash,
        String status,
        int attempts,
        int maxAttempts,
        String errorMessage,
        LocalDateTime nextRetryTime,
        LocalDateTime startTime,
        LocalDateTime finishTime,
        LocalDateTime createTime,
        LocalDateTime modifyTime
) {
}
