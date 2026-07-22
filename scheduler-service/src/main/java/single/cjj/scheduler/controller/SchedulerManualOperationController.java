package single.cjj.scheduler.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.scheduler.dto.ManualOperationRequest;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.entity.MatrixSchedulerOperationLog;
import single.cjj.scheduler.service.SchedulerManualOperationService;

@RestController
@RequestMapping("/scheduler/executions")
public class SchedulerManualOperationController {

    private final SchedulerManualOperationService operationService;

    public SchedulerManualOperationController(SchedulerManualOperationService operationService) {
        this.operationService = operationService;
    }

    @PostMapping("/{executionNo}/retry-now")
    public ApiResponse<MatrixSchedulerExecution> retryNow(
            @PathVariable("executionNo") String executionNo,
            @RequestHeader(value = "X-User-Id", required = false) String operatorId,
            @Valid @RequestBody ManualOperationRequest request) {
        return ApiResponse.success(operationService.retryNow(executionNo, operatorId, request.reason()));
    }

    @PostMapping("/{executionNo}/stop-retry")
    public ApiResponse<MatrixSchedulerExecution> stopRetry(
            @PathVariable("executionNo") String executionNo,
            @RequestHeader(value = "X-User-Id", required = false) String operatorId,
            @Valid @RequestBody ManualOperationRequest request) {
        return ApiResponse.success(operationService.stopRetry(executionNo, operatorId, request.reason()));
    }

    @PostMapping("/{executionNo}/cancel")
    public ApiResponse<MatrixSchedulerExecution> cancel(
            @PathVariable("executionNo") String executionNo,
            @RequestHeader(value = "X-User-Id", required = false) String operatorId,
            @Valid @RequestBody ManualOperationRequest request) {
        return ApiResponse.success(operationService.cancel(executionNo, operatorId, request.reason()));
    }

    @PostMapping("/{executionNo}/skip")
    public ApiResponse<MatrixSchedulerExecution> skip(
            @PathVariable("executionNo") String executionNo,
            @RequestHeader(value = "X-User-Id", required = false) String operatorId,
            @Valid @RequestBody ManualOperationRequest request) {
        return ApiResponse.success(operationService.skip(executionNo, operatorId, request.reason()));
    }

    @PostMapping("/{executionNo}/mark-success")
    public ApiResponse<MatrixSchedulerExecution> markSuccess(
            @PathVariable("executionNo") String executionNo,
            @RequestHeader(value = "X-User-Id", required = false) String operatorId,
            @Valid @RequestBody ManualOperationRequest request) {
        return ApiResponse.success(operationService.markSuccess(executionNo, operatorId, request.reason()));
    }

    @GetMapping("/{executionNo}/operation-logs")
    public ApiResponse<IPage<MatrixSchedulerOperationLog>> listLogs(
            @PathVariable("executionNo") String executionNo,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(operationService.listLogs(executionNo, page, size));
    }
}
