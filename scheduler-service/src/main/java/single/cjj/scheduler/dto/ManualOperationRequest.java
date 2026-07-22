package single.cjj.scheduler.dto;

import jakarta.validation.constraints.NotBlank;

public record ManualOperationRequest(
        @NotBlank(message = "操作原因不能为空") String reason) {
}
