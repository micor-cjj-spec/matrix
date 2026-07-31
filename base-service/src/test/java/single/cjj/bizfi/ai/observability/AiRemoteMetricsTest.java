package single.cjj.bizfi.ai.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiRemoteMetricsTest {

    @Test
    void shouldRecordAttemptsRetriesFallbackAndCircuitEvents() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiRemoteMetrics metrics = new AiRemoteMetrics(registry);

        metrics.recordAttempt("chat", URI.create("http://127.0.0.1:10020/api"));
        metrics.recordRetry("chat", "http-5xx");
        metrics.recordRequest("chat", "success", metrics.start());
        metrics.recordCircuitOpened();
        metrics.recordCircuitRejected("stream");
        metrics.recordFallback("stream");

        assertEquals(1.0, registry.get("matrix.ai.remote.attempts")
                .tags("operation", "chat", "endpoint", "127.0.0.1:10020")
                .counter().count());
        assertEquals(1.0, registry.get("matrix.ai.remote.retries")
                .tags("operation", "chat", "reason", "http-5xx")
                .counter().count());
        assertEquals(1.0, registry.get("matrix.ai.remote.requests")
                .tags("operation", "chat", "outcome", "success")
                .counter().count());
        assertEquals(1.0, registry.get("matrix.ai.remote.circuit.opens").counter().count());
        assertEquals(1.0, registry.get("matrix.ai.remote.circuit.rejections")
                .tag("operation", "stream").counter().count());
        assertEquals(1.0, registry.get("matrix.ai.remote.fallbacks")
                .tag("operation", "stream").counter().count());
    }
}
