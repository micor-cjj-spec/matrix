package single.cjj.bizfi.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {
    private String conversationId;
    private String userMessage;
    private List<String> kbIds;
    private Boolean stream;
}
