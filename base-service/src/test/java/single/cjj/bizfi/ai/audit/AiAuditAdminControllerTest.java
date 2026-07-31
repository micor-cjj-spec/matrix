package single.cjj.bizfi.ai.audit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAuditAdminControllerTest {

    private final AiAuditOperatorPermissionService permissionService = mock(
            AiAuditOperatorPermissionService.class
    );
    private final FinanceAiAuditClient auditClient = mock(FinanceAiAuditClient.class);
    private final AiAuditAdminController controller = new AiAuditAdminController(
            permissionService,
            auditClient
    );

    @Test
    void shouldRequireViewerForExecutionDetail() {
        AiAuditOperatorContext operator = viewer();
        AiToolExecutionAuditResponse expected = response();
        when(permissionService.requireViewer()).thenReturn(operator);
        when(auditClient.execution(operator, "tool_request_1")).thenReturn(expected);

        AiToolExecutionAuditResponse actual = controller.execution("tool_request_1");

        assertEquals(expected, actual);
        verify(permissionService).requireViewer();
    }

    @Test
    void shouldRequireViewerForSearch() {
        AiAuditOperatorContext operator = viewer();
        AiToolExecutionAuditPageResponse expected = new AiToolExecutionAuditPageResponse(
                1,
                20,
                1L,
                1L,
                List.of(response())
        );
        when(permissionService.requireViewer()).thenReturn(operator);
        when(auditClient.executions(
                operator,
                7L,
                10L,
                "2026-07",
                "TIMED_OUT",
                "c_tool",
                "trace_tool",
                null,
                null,
                1,
                20
        )).thenReturn(expected);

        AiToolExecutionAuditPageResponse actual = controller.executions(
                7L,
                10L,
                "2026-07",
                "TIMED_OUT",
                "c_tool",
                "trace_tool",
                null,
                null,
                1,
                20
        );

        assertEquals(expected, actual);
    }

    @Test
    void shouldRequireReconcilerForManualReconciliation() {
        AiAuditOperatorContext operator = new AiAuditOperatorContext(
                8L,
                Set.of("AI_TOOL_AUDIT_VIEW", "AI_TOOL_AUDIT_RECONCILE")
        );
        AiToolExecutionReconciliationResponse expected = new AiToolExecutionReconciliationResponse(
                "2026-07-31T10:00:00",
                2,
                1
        );
        when(permissionService.requireReconciler()).thenReturn(operator);
        when(auditClient.reconcileStale(operator)).thenReturn(expected);

        assertEquals(expected, controller.reconcileStale());
        verify(permissionService).requireReconciler();
    }

    private AiAuditOperatorContext viewer() {
        return new AiAuditOperatorContext(7L, Set.of("AI_TOOL_AUDIT_VIEW"));
    }

    private AiToolExecutionAuditResponse response() {
        return new AiToolExecutionAuditResponse(
                "tool_request_1",
                "c_tool",
                "gpt-tool-model",
                "trace_tool",
                "month-end-close-check",
                7L,
                10L,
                "2026-07",
                "TIMED_OUT",
                null,
                null,
                null,
                null,
                3600000L,
                "EXECUTION_TIMEOUT",
                "timeout",
                "2026-07-31T09:00:00",
                "2026-07-31T10:00:00"
        );
    }
}
