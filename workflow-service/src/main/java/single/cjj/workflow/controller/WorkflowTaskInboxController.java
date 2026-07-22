package single.cjj.workflow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.workflow.api.WorkflowContracts;
import single.cjj.workflow.service.WorkflowTaskQueryService;

import java.util.List;

@RestController
@RequestMapping("/workflow/tasks")
public class WorkflowTaskInboxController {

    private final WorkflowTaskQueryService taskQueryService;

    public WorkflowTaskInboxController(WorkflowTaskQueryService taskQueryService) {
        this.taskQueryService = taskQueryService;
    }

    @GetMapping
    public ApiResponse<List<WorkflowContracts.TaskResponse>> list(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("assigneeType") String assigneeType,
            @RequestParam("assigneeValue") String assigneeValue,
            @RequestParam(value = "status", defaultValue = "PENDING") String status,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ApiResponse.success(taskQueryService.listTasks(
                tenantId,
                assigneeType.toUpperCase(),
                assigneeValue,
                status.toUpperCase(),
                limit
        ));
    }
}
