package single.cjj.bizfi.ai.dto;

public record AiKnowledgeAccessResponse(
        String kbId,
        boolean aclEnabled,
        String permission,
        boolean canView,
        boolean canEdit,
        boolean canAdmin,
        boolean canOwn
) {
}
