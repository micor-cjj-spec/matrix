package single.cjj.workflow.controller;

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
import single.cjj.workflow.api.WorkflowContracts;
import single.cjj.workflow.service.WorkflowHistoryService;
import single.cjj.workflow.service.WorkflowService;

import java.util.List;

@RestController
@RequestMapping("/workflow")
public class WorkflowInstanceController {

    private final WorkflowService workflowService;
    private final WorkflowHistoryService historyService;

    public WorkflowInstanceController(WorkflowService workflowService,
                                      WorkflowHistoryService historyService) {
        this.workflowService = workflowService;
        this.historyService = historyService;
    }

    @PostMapping("/instances")
    public ApiResponse<WorkflowContracts.InstanceResponse> start(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WorkflowContracts.StartWorkflowRequest request) {
        return ApiResponse.success(workflowService.startWorkflow(request, idempotencyKey));
    }

    @PostMapping("/instances/{instanceId}/resubmit")
    public ApiResponse<WorkflowContracts.InstanceResponse> resubmit(
            @PathVariable("instanceId") String instanceId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody WorkflowContracts.ResubmitInstanceRequest request) {
        return ApiResponse.success(workflowService.resubmitInstance(instanceId, request, requestId));
    }

    @PostMapping("/instances/{instanceId}/cancel")
    public ApiResponse<WorkflowContracts.InstanceResponse> cancel(
            @PathVariable("instanceId") String instanceId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody WorkflowContracts.CancelInstanceRequest request) {
        return ApiResponse.success(workflowService.cancelInstance(instanceId, request, requestId));
    }

    @GetMapping("/instances/{instanceId}")
    public ApiResponse<WorkflowContracts.InstanceResponse> get(
            @PathVariable("instanceId") String instanceId) {
        return ApiResponse.success(workflowService.getInstance(instanceId));
    }

    @GetMapping("/instances/{instanceId}/tasks")
    public ApiResponse<List<WorkflowContracts.TaskResponse>> tasks(
            @PathVariable("instanceId") String instanceId) {
        return ApiResponse.success(historyService.listTasks(instanceId));
    }

    @GetMapping("/instances/{instanceId}/timeline")
    public ApiResponse<List<WorkflowContracts.TimelineResponse>> timeline(
            @PathVariable("instanceId") String instanceId) {
        return ApiResponse.success(historyService.listTimeline(instanceId));
    }

    @GetMapping("/business/{sourceSystem}/{businessType}/{businessId}")
    public ApiResponse<WorkflowContracts.InstanceResponse> getByBusiness(
            @PathVariable("sourceSystem") String sourceSystem,
            @PathVariable("businessType") String businessType,
            @PathVariable("businessId") String businessId,
            @RequestParam("tenantId") String tenantId) {
        return ApiResponse.success(workflowService.getBusinessInstance(
                tenantId, sourceSystem, businessType, businessId
        ));
    }
}
