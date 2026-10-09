package single.cjj.bizfi.ai.dto;

import lombok.Data;

@Data
public class AiKnowledgeAclRequest {
    private String subjectType;
    private String subjectId;
    private String permission;
}
