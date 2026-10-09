package single.cjj.bizfi.ai.dto;

import lombok.Data;

@Data
public class AiKnowledgeBaseRequest {
    private String kbId;
    private String name;
    private String description;
    private String status;
}
