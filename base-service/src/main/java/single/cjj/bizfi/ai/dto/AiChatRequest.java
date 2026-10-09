package single.cjj.bizfi.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {
    private String conversationId;
    private String userMessage;
    private List<String> kbIds;
    private Boolean stream;

    /**
     * Optional model-routing hint: general, knowledge-qa, financial-analysis,
     * tool-calling, or evaluation. Unknown values safely fall back to general.
     */
    private String taskType;

    /**
     * Server-approved tool name. The first supported tool is
     * month-end-close-check.
     */
    private String toolName;

    /**
     * Organization requested for the finance tool. It is authorized in
     * base-service before entering the model runtime.
     */
    private Long organizationId;

    /**
     * Accounting period in yyyy-MM format.
     */
    private String accountingPeriod;
}
