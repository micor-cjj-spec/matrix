package single.cjj.bizfi.ai.dto;

public record AiKnowledgeImportResponse(
        AiKnowledgeDocDetailResponse document,
        AiKnowledgeIndexJobResponse indexJob
) {
}
