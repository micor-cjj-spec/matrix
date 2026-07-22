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
import single.cjj.workflow.service.WorkflowService;

@RestController
@RequestMapping("/workflow")
public class WorkflowInstanceController {

    private final WorkflowService workflowService;

    public WorkflowInstanceController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/instances")
    public ApiResponse<WorkflowContracts.InstanceResponse> start(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WorkflowContracts.StartWorkflowRequest request) {
        return ApiResponse.success(workflowService.startWorkflow(request, idempotencyKey));
    }

    @GetMapping("/instances/{instanceId}")
    public ApiResponse<WorkflowContracts.InstanceResponse> get(
            @PathVariable("instanceId") String instanceId) {
        return ApiResponse.success(workflowService.getInstance(instanceId));
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
