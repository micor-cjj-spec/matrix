package single.cjj.scheduler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ExecutionProgressRequest(
        @NotBlank(message = "执行器实例不能为空") String executorInstance,
        @Min(value = 0, message = "进度不能小于0")
        @Max(value = 100, message = "进度不能大于100") Integer progress,
        String stage,
        String message) {
}
