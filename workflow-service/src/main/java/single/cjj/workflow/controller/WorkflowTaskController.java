package single.cjj.workflow.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.workflow.api.WorkflowContracts;
import single.cjj.workflow.service.WorkflowService;

@RestController
@RequestMapping("/workflow/tasks")
public class WorkflowTaskController {

    private final WorkflowService workflowService;

    public WorkflowTaskController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<WorkflowContracts.TaskResponse> get(
            @PathVariable("taskId") String taskId) {
        return ApiResponse.success(workflowService.getTask(taskId));
    }

    @PostMapping("/{taskId}/actions")
    public ApiResponse<WorkflowContracts.InstanceResponse> action(
            @PathVariable("taskId") String taskId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody WorkflowContracts.TaskActionRequest request) {
        return ApiResponse.success(workflowService.actOnTask(taskId, request, requestId));
    }
}
