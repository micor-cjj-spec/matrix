package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModelResult {
    private String answer;
    private String model;
    private String mode;
    private String traceId;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Double estimatedCost;
}
