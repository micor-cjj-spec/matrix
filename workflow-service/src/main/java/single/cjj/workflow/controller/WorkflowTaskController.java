package single.cjj.workflow.controller;

import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.workflow.security.SalesWorkflowTaskGuard;
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

    @Autowired(required=false)
    private SalesWorkflowTaskGuard salesTaskGuard;

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
            @RequestHeader(value = "Authorization", required = false) String bearer,
            @Valid @RequestBody WorkflowContracts.TaskActionRequest request) {
        if (StringUtils.hasText(trustedUserId)
                && !trustedUserId.trim().equals(request.operatorId())) {
            throw new BizException("请求用户与任务操作人不一致");
        }
        Set<String> roles=parseRoles(roleHeader);
        WorkflowContracts.TaskResponse task=workflowService.getTask(taskId);
        WorkflowContracts.InstanceResponse instance=workflowService.getInstance(task.instanceId());
        if ("MATRIX_ERP".equals(instance.sourceSystem())
                && Set.of("SALES_QUOTE","SALES_CONTRACT").contains(instance.businessType())) {
            if(salesTaskGuard==null)throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,"Sales workflow authorization is disabled");
            roles=salesTaskGuard.authorize(bearer,instance,request.operatorId());
        }
        return ApiResponse.success(workflowService.actOnTask(
                taskId, request, requestId, roles));
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
