package single.cjj.workflow.controller;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.workflow.api.WorkflowContracts;
import single.cjj.workflow.service.WorkflowTaskCenterService;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@RestController
@RequestMapping("/workflow/task-center")
public class WorkflowTaskCenterController {

    private final WorkflowTaskCenterService taskCenterService;

    public WorkflowTaskCenterController(WorkflowTaskCenterService taskCenterService) {
        this.taskCenterService = taskCenterService;
    }

    @GetMapping
    public ApiResponse<WorkflowContracts.TaskCenterPage> query(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roleHeader,
            @RequestParam("tenantId") String tenantId,
            @RequestParam(value = "view", defaultValue = "TODO") WorkflowContracts.TaskCenterView view,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(taskCenterService.query(
                tenantId, userId, parseRoles(roleHeader), view,
                businessType, keyword, page, size));
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
