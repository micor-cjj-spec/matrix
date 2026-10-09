package single.cjj.scheduler.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ExecutorRegisterRequest(
        @NotBlank String executorCode,
        @NotBlank String executorName,
        @NotBlank String instanceId,
        @Min(1) @Max(1000) Integer maxConcurrency,
        @NotEmpty List<@Valid HandlerRequest> handlers) {

    public record HandlerRequest(@NotBlank String handlerCode,
                                 @NotBlank String handlerName) {
    }
}
