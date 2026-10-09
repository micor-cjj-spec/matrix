package single.cjj.bizfi.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import single.cjj.bizfi.mapper.SalesRoleGrantMapper;
import single.cjj.bizfi.security.SalesAclRevision;
import java.util.Set;

/**
 * Only callers authenticated by SalesGrantAdminController may invoke this service.
 * Role changes are serialized per user/org using a Redis odd/even revision.
 */
@Service
@ConditionalOnProperty(prefix="matrix.sales-auth",name="admin-enabled",havingValue="true")
public class SalesGrantAdminService {
    private static final Set<String> ALLOWED = Set.of(
            "SALES_VIEWER","SALES_EDITOR","SALES_APPROVER","SALES_ADMIN");
    private final SalesRoleGrantMapper mapper;
    private final SalesAclRevision revisions;

    public SalesGrantAdminService(SalesRoleGrantMapper mapper, SalesAclRevision revisions) {
        this.mapper = mapper;
        this.revisions = revisions;
    }

    @Transactional(rollbackFor = Exception.class)
    public void change(String tenantId, Long orgId, Long userId, String role,
                       boolean grant, Long operatorId) {
        if (tenantId == null || tenantId.isBlank()
                || orgId == null || orgId <= 0 || userId == null || userId <= 0
                || operatorId == null || operatorId <= 0 || !ALLOWED.contains(role)) {
            throw new IllegalArgumentException("Illegal sales grant scope or role");
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Sales role mutation requires a database transaction");
        }

        // Move revision to odd BEFORE mutating grants, so old and in-flight JWTs
        // are blocked. Redis begin is atomic; concurrent changes are rejected.
        revisions.beginChange(tenantId, orgId, userId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                // On commit AND rollback, a fresh even revision invalidates old tokens.
                // Failure to publish leaves odd revision, denying sales access.
                revisions.finishChange(tenantId, orgId, userId);
            }
        });

        if (grant) {
            mapper.grant(IdWorker.getId(), tenantId, orgId, userId, role, operatorId);
        } else {
            mapper.revoke(tenantId, orgId, userId, role, operatorId);
        }
        mapper.appendAudit(IdWorker.getId(), tenantId, orgId, userId, role,
                grant ? "GRANT" : "REVOKE", operatorId);
    }
}
