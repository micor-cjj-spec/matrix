package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModelRequest {
    private String userMessage;
    private List<AiMessageResponse> historyMessages;
    private List<String> knowledgeSnippets;
    private String taskType;
    private AiToolContext toolContext;

    public AiModelRequest(
            String userMessage,
            List<AiMessageResponse> historyMessages,
            List<String> knowledgeSnippets
    ) {
        this(userMessage, historyMessages, knowledgeSnippets, null, null);
    }

    public AiModelRequest(
            String userMessage,
            List<AiMessageResponse> historyMessages,
            List<String> knowledgeSnippets,
            String taskType
    ) {
        this(userMessage, historyMessages, knowledgeSnippets, taskType, null);
    }
}
