package single.cjj.botp.execution;

import org.junit.jupiter.api.Test;
import single.cjj.botp.adapter.BotpAdapterRegistry;
import single.cjj.botp.adapter.InMemoryDemoDocumentAdapter;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.ExecutionMode;
import single.cjj.botp.domain.BotpContracts.ExecutionRequest;
import single.cjj.botp.domain.BotpContracts.ExecutionResult;
import single.cjj.botp.domain.BotpContracts.ExecutionStatus;
import single.cjj.botp.domain.BotpContracts.PreviewResult;
import single.cjj.botp.engine.BotpMappingEngine;
import single.cjj.botp.rule.InMemoryBotpRuleRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DefaultBotpExecutionServiceTest {

    @Test
    void shouldPreviewAndExecuteIdempotently() {
        InMemoryDemoDocumentAdapter adapter = new InMemoryDemoDocumentAdapter();
        DefaultBotpExecutionService service = new DefaultBotpExecutionService(
                new InMemoryBotpRuleRepository(),
                new BotpAdapterRegistry(List.of(adapter)),
                new BotpMappingEngine()
        );
        ExecutionRequest request = new ExecutionRequest(
                "REQUEST-001",
                "TEST_CLIENT",
                "default",
                "DEMO_ORDER_TO_DELIVERY",
                List.of(new DocumentRef("DEMO", "DEMO_ORDER", "ORDER-001", List.of())),
                Map.of("operatorId", "admin"),
                ExecutionMode.SYNC,
                null
        );

        PreviewResult preview = service.preview(request);
        ExecutionResult first = service.execute(request);
        ExecutionResult second = service.execute(request);

        assertEquals(1, preview.targetDrafts().size());
        assertEquals("ORDER-001", preview.targetDrafts().get(0).header().get("sourceOrderNo"));
        assertFalse(preview.targetDrafts().get(0).entries().isEmpty());
        assertEquals(ExecutionStatus.SUCCEEDED, first.status());
        assertEquals(first.executionId(), second.executionId());
        assertEquals(first.targetDocuments(), second.targetDocuments());
    }
}
