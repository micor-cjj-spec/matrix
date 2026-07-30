package single.cjj.matrix.ai.dto;

import lombok.Data;

@Data
public class AiChatRequest {

    private Long conversationId;

    private String scene;

    private String message;
}
