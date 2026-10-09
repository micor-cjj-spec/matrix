package single.cjj.bizfi.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Component
public class AiRemoteMetrics {

    private final MeterRegistry meterRegistry;

    public AiRemoteMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static AiRemoteMetrics isolated() {
        return new AiRemoteMetrics(new SimpleMeterRegistry());
    }

    public long start() {
        return System.nanoTime();
    }

    public void recordAttempt(String operation, URI endpoint) {
        Counter.builder("matrix.ai.remote.attempts")
                .description("Attempts to call an ai-service endpoint")
                .tags("operation", safe(operation), "endpoint", endpointTag(endpoint))
                .register(meterRegistry)
                .increment();
    }

    public void recordRetry(String operation, String reason) {
        Counter.builder("matrix.ai.remote.retries")
                .description("Retried ai-service calls")
                .tags("operation", safe(operation), "reason", safe(reason))
                .register(meterRegistry)
                .increment();
    }

    public void recordRequest(String operation, String outcome, long startedAtNanos) {
        Counter.builder("matrix.ai.remote.requests")
                .description("Logical ai-service requests")
                .tags("operation", safe(operation), "outcome", safe(outcome))
                .register(meterRegistry)
                .increment();
        Timer.builder("matrix.ai.remote.duration")
                .description("Logical ai-service request duration")
                .publishPercentileHistogram()
                .tags("operation", safe(operation), "outcome", safe(outcome))
                .register(meterRegistry)
                .record(Math.max(0L, System.nanoTime() - startedAtNanos), TimeUnit.NANOSECONDS);
    }

    public void recordCircuitOpened() {
        Counter.builder("matrix.ai.remote.circuit.opens")
                .description("ai-service circuit breaker open transitions")
                .register(meterRegistry)
                .increment();
    }

    public void recordCircuitRejected(String operation) {
        Counter.builder("matrix.ai.remote.circuit.rejections")
                .description("Requests rejected by the ai-service circuit breaker")
                .tag("operation", safe(operation))
                .register(meterRegistry)
                .increment();
    }

    public void recordFallback(String operation) {
        Counter.builder("matrix.ai.remote.fallbacks")
                .description("Fallbacks from Spring AI remote adapter to prompt-http")
                .tag("operation", safe(operation))
                .register(meterRegistry)
                .increment();
    }

    private String endpointTag(URI endpoint) {
        if (endpoint == null) {
            return "unknown";
        }
        String authority = endpoint.getAuthority();
        return authority == null || authority.isBlank() ? "unknown" : authority;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
