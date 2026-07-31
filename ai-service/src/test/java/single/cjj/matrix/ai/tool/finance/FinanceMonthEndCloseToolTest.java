package single.cjj.matrix.ai.tool.finance;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.observability.AiToolMetrics;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceMonthEndCloseToolTest {

    @Test
    void shouldReadSecuritySensitiveValuesOnlyFromToolContext() {
        FinanceToolClient client = mock(FinanceToolClient.class);
        FinanceMonthEndCloseResult expected = new FinanceMonthEndCloseResult(
                10L,
                "2026-07",
                "OPEN",
                "BLOCKED",
                60,
                false,
                3,
                1,
                0,
                2,
                0,
                12,
                8,
                4,
                0,
                "2026-07-31T09:00:00",
                List.of(),
                List.of("存在未过账凭证"),
                true
        );
        when(client.monthEndCloseCheck(any())).thenReturn(expected);
        FinanceMonthEndCloseTool tool = new FinanceMonthEndCloseTool(
                client,
                new AiToolMetrics(new SimpleMeterRegistry())
        );

        FinanceMonthEndCloseResult actual = tool.monthEndCloseCheck(new ToolContext(Map.of(
                "toolName", "month-end-close-check",
                "requestedByUserId", 7L,
                "organizationId", 10L,
                "period", "2026-07",
                "requestId", "tool_request"
        )));

        assertEquals(expected, actual);
        ArgumentCaptor<ModelContracts.ToolContext> captor = ArgumentCaptor.forClass(
                ModelContracts.ToolContext.class
        );
        verify(client).monthEndCloseCheck(captor.capture());
        assertEquals(7L, captor.getValue().requestedByUserId());
        assertEquals(10L, captor.getValue().organizationId());
        assertEquals("2026-07", captor.getValue().period());
    }
}
