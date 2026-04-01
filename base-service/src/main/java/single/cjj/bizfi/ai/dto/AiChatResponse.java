package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private String conversationId;
    private String answer;
    private List<AiCitationResponse> citations;
    private AiUsageResponse usage;
    private String traceId;
    private String mode;
    private String model;
}
