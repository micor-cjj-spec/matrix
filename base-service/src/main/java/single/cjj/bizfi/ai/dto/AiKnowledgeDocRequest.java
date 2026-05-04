package single.cjj.bizfi.ai.dto;

import lombok.Data;

@Data
public class AiKnowledgeDocRequest {
    private String docId;
    private String title;
    private String category;
    private String sourcePath;
    private String content;
    private String version;
    private String status;
}
