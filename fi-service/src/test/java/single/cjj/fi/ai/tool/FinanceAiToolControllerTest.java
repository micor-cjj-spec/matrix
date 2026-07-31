package single.cjj.fi.ai.tool;

import org.junit.jupiter.api.Test;
import single.cjj.fi.ai.tool.audit.FinanceAiToolAuditService;
import single.cjj.fi.gl.service.BizfiFiPeriodProcessService;
import single.cjj.fi.gl.vo.MonthEndWorkbenchResultVO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceAiToolControllerTest {

    private final FinanceAiToolTokenGuard tokenGuard = mock(FinanceAiToolTokenGuard.class);
    private final BizfiFiPeriodProcessService periodProcessService = mock(BizfiFiPeriodProcessService.class);
    private final FinanceMonthEndCloseToolMapper mapper = mock(FinanceMonthEndCloseToolMapper.class);
    private final FinanceAiToolAuditService auditService = mock(FinanceAiToolAuditService.class);
    private final FinanceAiToolController controller = new FinanceAiToolController(
            tokenGuard,
            periodProcessService,
            mapper,
            auditService
    );

    @Test
    void shouldAuditSuccessfulExecution() {
        FinanceMonthEndCloseToolRequest request = request();
        MonthEndWorkbenchResultVO workbench = new MonthEndWorkbenchResultVO();
        FinanceMonthEndCloseToolResponse response = response();
        when(periodProcessService.monthEndWorkbench(10L, "2026-07")).thenReturn(workbench);
        when(mapper.map(workbench)).thenReturn(response);

        FinanceMonthEndCloseToolResponse actual = controller.monthEndCloseCheck("secret", request);

        assertEquals(response, actual);
        verify(tokenGuard).verify("secret");
        verify(auditService).recordStarted("month-end-close-check", request);
        verify(auditService).recordSucceeded(
                org.mockito.ArgumentMatchers.eq("month-end-close-check"),
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.eq(response),
                anyLong()
        );
    }

    @Test
    void shouldAuditFailedExecutionAndPreserveOriginalFailure() {
        FinanceMonthEndCloseToolRequest request = request();
        IllegalStateException failure = new IllegalStateException("month-end failed");
        when(periodProcessService.monthEndWorkbench(10L, "2026-07")).thenThrow(failure);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> controller.monthEndCloseCheck("secret", request)
        );

        assertEquals(failure, actual);
        verify(auditService).recordFailed(
                org.mockito.ArgumentMatchers.eq("month-end-close-check"),
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.same(failure),
                anyLong()
        );
    }

    private FinanceMonthEndCloseToolRequest request() {
        return new FinanceMonthEndCloseToolRequest(
                7L,
                10L,
                "2026-07",
                "tool_request_1",
                "c_tool",
                "gpt-tool-model",
                "trace_tool"
        );
    }

    private FinanceMonthEndCloseToolResponse response() {
        return new FinanceMonthEndCloseToolResponse(
                10L,
                "2026-07",
                "OPEN",
                "BLOCKED",
                70,
                false,
                5,
                2,
                1,
                2,
                0,
                10,
                8,
                2,
                0,
                "2026-07-31T10:00:00",
                List.of(),
                List.of(),
                true
        );
    }
}
