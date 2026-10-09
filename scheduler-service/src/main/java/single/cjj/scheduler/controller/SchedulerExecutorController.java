package single.cjj.scheduler.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.scheduler.dto.ExecutorHeartbeatRequest;
import single.cjj.scheduler.dto.ExecutorRegisterRequest;
import single.cjj.scheduler.entity.MatrixSchedulerExecutor;
import single.cjj.scheduler.entity.MatrixSchedulerExecutorInstance;
import single.cjj.scheduler.entity.MatrixSchedulerHandler;
import single.cjj.scheduler.service.ExecutorRegistryService;

import java.util.List;

@RestController
@RequestMapping("/scheduler/executors")
public class SchedulerExecutorController {

    private final ExecutorRegistryService registryService;

    public SchedulerExecutorController(ExecutorRegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping("/register")
    public ApiResponse<MatrixSchedulerExecutor> register(
            @Valid @RequestBody ExecutorRegisterRequest request) {
        return ApiResponse.success(registryService.register(request));
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Boolean> heartbeat(
            @Valid @RequestBody ExecutorHeartbeatRequest request) {
        registryService.heartbeat(request);
        return ApiResponse.success(true);
    }

    @GetMapping
    public ApiResponse<List<MatrixSchedulerExecutor>> list() {
        return ApiResponse.success(registryService.listExecutors());
    }

    @GetMapping("/{executorCode}/instances")
    public ApiResponse<List<MatrixSchedulerExecutorInstance>> instances(
            @PathVariable String executorCode) {
        return ApiResponse.success(registryService.listInstances(executorCode));
    }

    @GetMapping("/{executorCode}/handlers")
    public ApiResponse<List<MatrixSchedulerHandler>> handlers(
            @PathVariable String executorCode) {
        return ApiResponse.success(registryService.listHandlers(executorCode));
    }
}
