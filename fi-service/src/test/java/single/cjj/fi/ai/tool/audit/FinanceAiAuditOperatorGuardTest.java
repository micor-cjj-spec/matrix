package single.cjj.fi.ai.tool.audit;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.fi.ai.tool.FinanceAiToolProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceAiAuditOperatorGuardTest {

    @Test
    void shouldAuthorizeViewerWithDedicatedToken() {
        FinanceAiAuditOperatorGuard guard = guard();

        FinanceAiAuditOperator operator = guard.requireViewer(
                "audit-secret",
                "7",
                "AI_TOOL_AUDIT_VIEW",
                "audit_request_1"
        );

        assertEquals("7", operator.operatorId());
        assertTrue(operator.roles().contains(FinanceAiAuditOperatorGuard.VIEW_ROLE));
    }

    @Test
    void shouldAllowReconcilerToView() {
        FinanceAiAuditOperatorGuard guard = guard();

        FinanceAiAuditOperator operator = guard.requireViewer(
                "audit-secret",
                "8",
                "AI_TOOL_AUDIT_RECONCILE",
                "audit_request_2"
        );

        assertTrue(operator.roles().contains(FinanceAiAuditOperatorGuard.RECONCILE_ROLE));
    }

    @Test
    void shouldRejectViewerForReconciliation() {
        FinanceAiAuditOperatorGuard guard = guard();

        assertThrows(ResponseStatusException.class, () -> guard.requireReconciler(
                "audit-secret",
                "7",
                "AI_TOOL_AUDIT_VIEW",
                "audit_request_3"
        ));
    }

    @Test
    void shouldRejectToolTokenOnAuditEndpoint() {
        FinanceAiAuditOperatorGuard guard = guard();

        assertThrows(ResponseStatusException.class, () -> guard.requireViewer(
                "tool-secret",
                "7",
                "AI_TOOL_AUDIT_VIEW",
                "audit_request_4"
        ));
    }

    private FinanceAiAuditOperatorGuard guard() {
        FinanceAiToolProperties properties = new FinanceAiToolProperties();
        properties.setInternalToken("tool-secret");
        properties.setAuditInternalToken("audit-secret");
        return new FinanceAiAuditOperatorGuard(properties);
    }
}
