package single.cjj.bizfi.ai.audit;

public interface AiAuditOperatorPermissionService {

    AiAuditOperatorContext requireViewer();

    AiAuditOperatorContext requireReconciler();
}
