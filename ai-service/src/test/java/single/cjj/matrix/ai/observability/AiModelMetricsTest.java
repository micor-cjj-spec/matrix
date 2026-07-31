package single.cjj.matrix.ai.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import single.cjj.matrix.ai.service.AiTaskRouter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiModelMetricsTest {

    @Test
    void shouldRecordSuccessTokensAndFailure() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiModelMetrics metrics = new AiModelMetrics(registry);
        AiTaskRouter.ModelRoute route = new AiTaskRouter.ModelRoute("knowledge-qa", "qa-model");

        metrics.recordSuccess("chat", route, metrics.start(), 12, 8);
        metrics.recordFailure("stream", route, metrics.start(), "TimeoutException");

        assertEquals(1.0, registry.get("matrix.ai.model.requests")
                .tags("operation", "chat", "task", "knowledge-qa", "model", "qa-model", "outcome", "success")
                .counter().count());
        assertEquals(12.0, registry.get("matrix.ai.model.tokens")
                .tags("task", "knowledge-qa", "model", "qa-model", "type", "prompt")
                .counter().count());
        assertEquals(1.0, registry.get("matrix.ai.model.errors")
                .tags("operation", "stream", "task", "knowledge-qa", "model", "qa-model", "error", "TimeoutException")
                .counter().count());
    }
}
