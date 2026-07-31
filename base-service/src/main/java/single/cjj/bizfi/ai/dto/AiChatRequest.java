package single.cjj.bizfi.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {
    private String conversationId;
    private String userMessage;
    private List<String> kbIds;
    private Boolean stream;

    /**
     * Optional model-routing hint: general, knowledge-qa, financial-analysis,
     * tool-calling, or evaluation. Unknown values safely fall back to general.
     */
    private String taskType;
}
