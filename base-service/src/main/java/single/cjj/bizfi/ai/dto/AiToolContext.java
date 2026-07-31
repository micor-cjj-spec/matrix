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
    private String conversationId;

    public AiToolContext(
            String toolName,
            Long requestedByUserId,
            Long organizationId,
            String period,
            String requestId
    ) {
        this(toolName, requestedByUserId, organizationId, period, requestId, null);
    }
}
