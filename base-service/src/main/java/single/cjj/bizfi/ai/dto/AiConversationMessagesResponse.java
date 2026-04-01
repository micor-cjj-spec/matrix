package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiConversationMessagesResponse {
    private String conversationId;
    private List<AiMessageResponse> messages;
}
