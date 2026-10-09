package single.cjj.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExecutionCallbackRequest {

    @NotBlank(message = "执行状态不能为空")
    private String status;
    private String executorInstance;
    private String responsePayload;
    private String errorCode;
    private String errorMessage;
}
