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
}
