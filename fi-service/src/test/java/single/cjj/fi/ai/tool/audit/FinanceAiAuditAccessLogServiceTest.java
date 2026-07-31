package single.cjj.fi.ai.tool.audit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FinanceAiAuditAccessLogServiceTest {

    @Test
    void shouldPersistBoundedOperatorAccess() {
        FinanceAiAuditAccessLogMapper mapper = mock(FinanceAiAuditAccessLogMapper.class);
        FinanceAiAuditAccessLogService service = new FinanceAiAuditAccessLogService(mapper);
        FinanceAiAuditOperator operator = new FinanceAiAuditOperator(
                "7",
                Set.of("AI_TOOL_AUDIT_VIEW"),
                "audit_request_1"
        );

        service.recordRequired(
                operator,
                FinanceAiAuditAccessLogService.SEARCH,
                "organizationId=10;period=2026-07",
                FinanceAiAuditAccessLogService.SUCCESS,
                3,
                25,
                null
        );

        ArgumentCaptor<FinanceAiAuditAccessLog> captor = ArgumentCaptor.forClass(
                FinanceAiAuditAccessLog.class
        );
        verify(mapper).insert(captor.capture());
        FinanceAiAuditAccessLog record = captor.getValue();
        assertEquals("7", record.getFoperatorid());
        assertEquals("SEARCH", record.getFaction());
        assertEquals("SUCCESS", record.getFoutcome());
        assertEquals(3L, record.getFresultcount());
    }

    @Test
    void shouldFailClosedWhenAccessLogCannotBeWritten() {
        FinanceAiAuditAccessLogMapper mapper = mock(FinanceAiAuditAccessLogMapper.class);
        doThrow(new IllegalStateException("database unavailable"))
                .when(mapper).insert(org.mockito.ArgumentMatchers.any());
        FinanceAiAuditAccessLogService service = new FinanceAiAuditAccessLogService(mapper);

        assertThrows(ResponseStatusException.class, () -> service.recordRequired(
                new FinanceAiAuditOperator("7", Set.of("AI_TOOL_AUDIT_VIEW"), "audit_request_2"),
                FinanceAiAuditAccessLogService.DETAIL,
                "requestId=tool_1",
                FinanceAiAuditAccessLogService.SUCCESS,
                1,
                10,
                null
        ));
    }
}
