package single.cjj.fi.ai.tool.audit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceAiAuditControllerTest {

    private final FinanceAiAuditOperatorGuard operatorGuard = mock(FinanceAiAuditOperatorGuard.class);
    private final FinanceAiToolAuditService auditService = mock(FinanceAiToolAuditService.class);
    private final FinanceAiAuditAccessLogService accessLogService = mock(FinanceAiAuditAccessLogService.class);
    private final FinanceAiAuditReconciliationCoordinator reconciliationCoordinator = mock(
            FinanceAiAuditReconciliationCoordinator.class
    );
    private final FinanceAiAuditController controller = new FinanceAiAuditController(
            operatorGuard,
            auditService,
            accessLogService,
            reconciliationCoordinator
    );

    @Test
    void shouldReturnAndAuditExecutionDetail() {
        FinanceAiAuditOperator operator = viewer();
        FinanceAiToolExecution execution = execution();
        when(operatorGuard.requireViewer("token", "7", "AI_TOOL_AUDIT_VIEW", "audit_request_1"))
                .thenReturn(operator);
        when(auditService.findByRequestId("tool_request_1")).thenReturn(Optional.of(execution));

        FinanceAiToolExecutionResponse response = controller.execution(
                "token",
                "7",
                "AI_TOOL_AUDIT_VIEW",
                "audit_request_1",
                "tool_request_1"
        );

        assertEquals("tool_request_1", response.requestId());
        verify(accessLogService).recordRequired(
                eq(operator),
                eq(FinanceAiAuditAccessLogService.DETAIL),
                eq("requestId=tool_request_1"),
                eq(FinanceAiAuditAccessLogService.SUCCESS),
                eq(1L),
                anyLong(),
                eq(null)
        );
    }

    @Test
    void shouldReturnAndAuditFilteredPage() {
        FinanceAiAuditOperator operator = viewer();
        FinanceAiToolExecutionPageResponse expected = FinanceAiToolExecutionPageResponse.of(
                1,
                20,
                1,
                List.of(execution())
        );
        when(operatorGuard.requireViewer("token", "7", "AI_TOOL_AUDIT_VIEW", "audit_request_2"))
                .thenReturn(operator);
        when(auditService.query(any(FinanceAiToolExecutionQuery.class))).thenReturn(expected);

        FinanceAiToolExecutionPageResponse actual = controller.executions(
                "token",
                "7",
                "AI_TOOL_AUDIT_VIEW",
                "audit_request_2",
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
        verify(accessLogService).recordRequired(
                eq(operator),
                eq(FinanceAiAuditAccessLogService.SEARCH),
                any(String.class),
                eq(FinanceAiAuditAccessLogService.SUCCESS),
                eq(1L),
                anyLong(),
                eq(null)
        );
    }

    @Test
    void shouldRequireReconcilerForManualReconciliation() {
        FinanceAiAuditOperator operator = new FinanceAiAuditOperator(
                "8",
                Set.of(FinanceAiAuditOperatorGuard.RECONCILE_ROLE),
                "audit_request_3"
        );
        FinanceAiAuditReconciliationResponse expected = new FinanceAiAuditReconciliationResponse(
                "2026-07-31T10:00:00",
                2,
                1
        );
        when(operatorGuard.requireReconciler(
                "token",
                "8",
                "AI_TOOL_AUDIT_RECONCILE",
                "audit_request_3"
        )).thenReturn(operator);
        when(reconciliationCoordinator.reconcile(operator)).thenReturn(expected);

        FinanceAiAuditReconciliationResponse actual = controller.reconcileStale(
                "token",
                "8",
                "AI_TOOL_AUDIT_RECONCILE",
                "audit_request_3"
        );

        assertEquals(expected, actual);
    }

    private FinanceAiAuditOperator viewer() {
        return new FinanceAiAuditOperator(
                "7",
                Set.of(FinanceAiAuditOperatorGuard.VIEW_ROLE),
                "audit_request"
        );
    }

    private FinanceAiToolExecution execution() {
        FinanceAiToolExecution execution = new FinanceAiToolExecution();
        execution.setFrequestid("tool_request_1");
        execution.setFconversationid("c_tool");
        execution.setFmodeltraceid("trace_tool");
        execution.setFstatus("TIMED_OUT");
        return execution;
    }
}
