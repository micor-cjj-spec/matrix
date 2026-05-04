package single.cjj.bizfi.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiKnowledgeRetrieveRequest {
    private String question;
    private List<String> kbIds;
    private Integer topK;
}
