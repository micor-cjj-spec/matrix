package single.cjj.bizfi.ai.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/admin/tool-executions")
public class AiAuditAdminController {

    private final AiAuditOperatorPermissionService permissionService;
    private final FinanceAiAuditClient auditClient;

    public AiAuditAdminController(
            AiAuditOperatorPermissionService permissionService,
            FinanceAiAuditClient auditClient
    ) {
        this.permissionService = permissionService;
        this.auditClient = auditClient;
    }

    @GetMapping("/{requestId}")
    public AiToolExecutionAuditResponse execution(@PathVariable("requestId") String requestId) {
        return auditClient.execution(permissionService.requireViewer(), requestId);
    }

    @GetMapping
    public AiToolExecutionAuditPageResponse executions(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "organizationId", required = false) Long organizationId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "modelTraceId", required = false) String modelTraceId,
            @RequestParam(value = "createdFrom", required = false) String createdFrom,
            @RequestParam(value = "createdTo", required = false) String createdTo,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size
    ) {
        return auditClient.executions(
                permissionService.requireViewer(),
                userId,
                organizationId,
                period,
                status,
                conversationId,
                modelTraceId,
                createdFrom,
                createdTo,
                page,
                size
        );
    }

    @PostMapping("/reconcile-stale")
    public AiToolExecutionReconciliationResponse reconcileStale() {
        return auditClient.reconcileStale(permissionService.requireReconciler());
    }
}
