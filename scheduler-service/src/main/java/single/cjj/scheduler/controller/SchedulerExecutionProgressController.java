package single.cjj.scheduler.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.scheduler.dto.ExecutionProgressRequest;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.service.SchedulerExecutionProgressService;

@RestController
@RequestMapping("/scheduler/callback/executions")
public class SchedulerExecutionProgressController {

    private final SchedulerExecutionProgressService progressService;

    public SchedulerExecutionProgressController(SchedulerExecutionProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping("/{executionNo}/progress")
    public ApiResponse<MatrixSchedulerExecution> progress(
            @PathVariable("executionNo") String executionNo,
            @RequestHeader("X-Executor-Code") String executorCode,
            @Valid @RequestBody ExecutionProgressRequest request) {
        return ApiResponse.success(progressService.report(executionNo, executorCode, request));
    }
}
