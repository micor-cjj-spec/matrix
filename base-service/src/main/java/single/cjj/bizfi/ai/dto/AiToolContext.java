package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiToolContext {
    private String toolName;
    private Long requestedByUserId;
    private Long organizationId;
    private String period;
    private String requestId;
}
