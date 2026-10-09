package single.cjj.scheduler.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.scheduler.dto.ExecutionCallbackRequest;
import single.cjj.scheduler.dto.SchedulerJobRequest;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.entity.MatrixSchedulerJob;
import single.cjj.scheduler.service.SchedulerDispatchService;
import single.cjj.scheduler.service.SchedulerJobService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/scheduler")
public class SchedulerJobController {

    private final SchedulerJobService jobService;
    private final SchedulerDispatchService dispatchService;

    public SchedulerJobController(SchedulerJobService jobService,
                                  SchedulerDispatchService dispatchService) {
        this.jobService = jobService;
        this.dispatchService = dispatchService;
    }

    @PostMapping("/jobs")
    public ApiResponse<MatrixSchedulerJob> create(@Valid @RequestBody SchedulerJobRequest request) {
        return ApiResponse.success(jobService.create(request, "PLATFORM", "matrix-web", null));
    }

    @PutMapping("/jobs/{jobId}")
    public ApiResponse<MatrixSchedulerJob> update(@PathVariable("jobId") Long jobId,
                                                   @Valid @RequestBody SchedulerJobRequest request) {
        return ApiResponse.success(jobService.update(jobId, request));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<MatrixSchedulerJob> get(@PathVariable("jobId") Long jobId) {
        return ApiResponse.success(jobService.get(jobId));
    }

    @GetMapping("/jobs")
    public ApiResponse<IPage<MatrixSchedulerJob>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sourceService", required = false) String sourceService) {
        return ApiResponse.success(jobService.list(page, size, keyword, status, sourceService));
    }

    @PostMapping("/jobs/{jobId}/pause")
    public ApiResponse<MatrixSchedulerJob> pause(@PathVariable("jobId") Long jobId) {
        return ApiResponse.success(jobService.pause(jobId));
    }

    @PostMapping("/jobs/{jobId}/resume")
    public ApiResponse<MatrixSchedulerJob> resume(@PathVariable("jobId") Long jobId) {
        return ApiResponse.success(jobService.resume(jobId));
    }

    @PostMapping("/jobs/{jobId}/run-now")
    public ApiResponse<Boolean> runNow(@PathVariable("jobId") Long jobId) {
        return ApiResponse.success(jobService.runNow(jobId));
    }

    @DeleteMapping("/jobs/{jobId}")
    public ApiResponse<Boolean> delete(@PathVariable("jobId") Long jobId) {
        return ApiResponse.success(jobService.delete(jobId));
    }

    @GetMapping("/cron/preview")
    public ApiResponse<List<LocalDateTime>> preview(
            @RequestParam("cron") String cron,
            @RequestParam(value = "timezone", defaultValue = "Asia/Shanghai") String timezone,
            @RequestParam(value = "count", defaultValue = "5") Integer count) {
        return ApiResponse.success(jobService.preview(cron, timezone, count));
    }

    @GetMapping("/executions")
    public ApiResponse<IPage<MatrixSchedulerExecution>> listExecutions(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "jobId", required = false) Long jobId,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.success(dispatchService.listExecutions(page, size, jobId, status));
    }

    @GetMapping("/executions/{executionNo}")
    public ApiResponse<MatrixSchedulerExecution> getExecution(
            @PathVariable("executionNo") String executionNo) {
        return ApiResponse.success(dispatchService.getExecution(executionNo));
    }

    @PostMapping("/callback/executions/{executionNo}")
    public ApiResponse<MatrixSchedulerExecution> callback(
            @PathVariable("executionNo") String executionNo,
            @RequestHeader("X-Executor-Code") String executorCode,
            @Valid @RequestBody ExecutionCallbackRequest request) {
        MatrixSchedulerExecution execution = dispatchService.getExecution(executionNo);
        if (!executorCode.equals(execution.getFexecutorCode())) {
            throw new IllegalArgumentException("执行器无权回调该执行实例");
        }
        return ApiResponse.success(dispatchService.callback(executionNo, request));
    }
}
