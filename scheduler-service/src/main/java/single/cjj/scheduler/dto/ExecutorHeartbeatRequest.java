package single.cjj.scheduler.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ExecutorHeartbeatRequest(
        @NotBlank String executorCode,
        @NotBlank String instanceId,
        @Min(0) Integer runningCount) {
}
