package single.cjj.matrix.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatResponse {

    private Long conversationId;

    private String answer;
}
