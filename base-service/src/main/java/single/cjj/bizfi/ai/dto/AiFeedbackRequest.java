package single.cjj.bizfi.ai.dto;

import lombok.Data;

@Data
public class AiFeedbackRequest {
    private String conversationId;
    private Long messageId;
    private String feedbackType;
    private String comment;
}
