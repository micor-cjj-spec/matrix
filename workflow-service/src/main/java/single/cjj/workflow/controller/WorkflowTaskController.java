package single.cjj.workflow.controller;

import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.workflow.api.WorkflowContracts;
import single.cjj.workflow.service.WorkflowService;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

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
            @RequestHeader(value = "X-User-Id", required = false) String trustedUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String roleHeader,
            @Valid @RequestBody WorkflowContracts.TaskActionRequest request) {
        if (StringUtils.hasText(trustedUserId)
                && !trustedUserId.trim().equals(request.operatorId())) {
            throw new BizException("请求用户与任务操作人不一致");
        }
        return ApiResponse.success(workflowService.actOnTask(
                taskId, request, requestId, parseRoles(roleHeader)));
    }

    private Set<String> parseRoles(String roleHeader) {
        if (!StringUtils.hasText(roleHeader)) {
            return Set.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        Arrays.stream(roleHeader.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(roles::add);
        return roles;
    }
}
