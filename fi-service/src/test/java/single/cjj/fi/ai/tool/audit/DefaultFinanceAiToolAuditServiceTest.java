package single.cjj.fi.ai.tool.audit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import single.cjj.fi.ai.tool.FinanceMonthEndCloseToolRequest;
import single.cjj.fi.ai.tool.FinanceMonthEndCloseToolResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals("c_tool", execution.getFconversationid());
        assertEquals("gpt-tool-model", execution.getFmodelname());
        assertEquals("trace_tool", execution.getFmodeltraceid());
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
        FinanceAiToolExecution execution = execution();
        when(mapper.selectOne(any())).thenReturn(execution);

        assertTrue(service.findByRequestId("tool_request_1").isPresent());
    }

    @Test
    void shouldQueryCorrelatedExecutionsWithBoundedPagination() {
        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.selectList(any())).thenReturn(List.of(execution()));

        FinanceAiToolExecutionPageResponse page = service.query(new FinanceAiToolExecutionQuery(
                7L,
                10L,
                "2026-07",
                "succeeded",
                "c_tool",
                "trace_tool",
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 31, 23, 59),
                1,
                500
        ));

        assertEquals(1, page.page());
        assertEquals(100, page.size());
        assertEquals(1, page.total());
        assertEquals(1, page.totalPages());
        assertEquals(1, page.items().size());
        assertEquals("c_tool", page.items().get(0).conversationId());
        assertEquals("gpt-tool-model", page.items().get(0).modelName());
        assertEquals("trace_tool", page.items().get(0).modelTraceId());
    }

    @Test
    void shouldRejectUnsupportedStatusAndInvertedTimeRange() {
        assertThrows(IllegalArgumentException.class, () -> service.query(new FinanceAiToolExecutionQuery(
                null, null, null, "UNKNOWN", null, null, null, null, 1, 20
        )));
        assertThrows(IllegalArgumentException.class, () -> service.query(new FinanceAiToolExecutionQuery(
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 7, 1, 0, 0),
                1,
                20
        )));
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

    private FinanceAiToolExecution execution() {
        FinanceAiToolExecution execution = new FinanceAiToolExecution();
        execution.setFrequestid("tool_request_1");
        execution.setFconversationid("c_tool");
        execution.setFmodelname("gpt-tool-model");
        execution.setFmodeltraceid("trace_tool");
        execution.setFtoolname("month-end-close-check");
        execution.setFuserid(7L);
        execution.setForganizationid(10L);
        execution.setFperiod("2026-07");
        execution.setFstatus("SUCCEEDED");
        execution.setFcreatetime(LocalDateTime.of(2026, 7, 31, 10, 0));
        return execution;
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
