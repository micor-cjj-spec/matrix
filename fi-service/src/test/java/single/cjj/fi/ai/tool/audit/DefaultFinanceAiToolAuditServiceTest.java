package single.cjj.fi.ai.tool.audit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import single.cjj.fi.ai.tool.FinanceMonthEndCloseToolRequest;
import single.cjj.fi.ai.tool.FinanceMonthEndCloseToolResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultFinanceAiToolAuditServiceTest {

    private final FinanceAiToolExecutionMapper mapper = mock(FinanceAiToolExecutionMapper.class);
    private final DefaultFinanceAiToolAuditService service = new DefaultFinanceAiToolAuditService(mapper);

    @Test
    void shouldPersistTrustedExecutionContextOnStart() {
        FinanceMonthEndCloseToolRequest request = request();

        service.recordStarted("month-end-close-check", request);

        ArgumentCaptor<FinanceAiToolExecution> captor = ArgumentCaptor.forClass(FinanceAiToolExecution.class);
        verify(mapper).insert(captor.capture());
        FinanceAiToolExecution execution = captor.getValue();
        assertEquals("tool_request_1", execution.getFrequestid());
        assertEquals("month-end-close-check", execution.getFtoolname());
        assertEquals(7L, execution.getFuserid());
        assertEquals(10L, execution.getForganizationid());
        assertEquals("2026-07", execution.getFperiod());
        assertEquals("STARTED", execution.getFstatus());
    }

    @Test
    void shouldUpdateSuccessfulExecution() {
        service.recordSucceeded(
                "month-end-close-check",
                request(),
                response(),
                123L
        );

        verify(mapper).update(any(), any());
    }

    @Test
    void shouldUpdateFailedExecution() {
        service.recordFailed(
                "month-end-close-check",
                request(),
                new IllegalStateException("finance failed\nwith detail"),
                50L
        );

        verify(mapper).update(any(), any());
    }

    @Test
    void shouldFindExecutionByRequestId() {
        FinanceAiToolExecution execution = new FinanceAiToolExecution();
        execution.setFrequestid("tool_request_1");
        when(mapper.selectOne(any())).thenReturn(execution);

        assertTrue(service.findByRequestId("tool_request_1").isPresent());
    }

    private FinanceMonthEndCloseToolRequest request() {
        return new FinanceMonthEndCloseToolRequest(7L, 10L, "2026-07", "tool_request_1");
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
                List.of("存在阻塞项"),
                true
        );
    }
}
