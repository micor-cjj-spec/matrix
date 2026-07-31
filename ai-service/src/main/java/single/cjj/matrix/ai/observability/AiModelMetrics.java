package single.cjj.matrix.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import single.cjj.matrix.ai.service.AiTaskRouter;

import java.util.concurrent.TimeUnit;

@Component
public class AiModelMetrics {

    private final MeterRegistry meterRegistry;

    public AiModelMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public long start() {
        return System.nanoTime();
    }

    public void recordSuccess(
            String operation,
            AiTaskRouter.ModelRoute route,
            long startedAtNanos,
            Integer promptTokens,
            Integer completionTokens
    ) {
        recordRequest(operation, route, "success", startedAtNanos);
        incrementTokens(route, "prompt", promptTokens);
        incrementTokens(route, "completion", completionTokens);
    }

    public void recordFailure(
            String operation,
            AiTaskRouter.ModelRoute route,
            long startedAtNanos,
            String errorType
    ) {
        Counter.builder("matrix.ai.model.errors")
                .description("AI model request failures")
                .tags(
                        "operation", safe(operation),
                        "task", safe(route.taskType()),
                        "model", safe(route.model()),
                        "error", safe(errorType)
                )
                .register(meterRegistry)
                .increment();
        recordRequest(operation, route, "failure", startedAtNanos);
    }

    private void recordRequest(
            String operation,
            AiTaskRouter.ModelRoute route,
            String outcome,
            long startedAtNanos
    ) {
        Counter.builder("matrix.ai.model.requests")
                .description("AI model requests")
                .tags(
                        "operation", safe(operation),
                        "task", safe(route.taskType()),
                        "model", safe(route.model()),
                        "outcome", outcome
                )
                .register(meterRegistry)
                .increment();

        Timer.builder("matrix.ai.model.duration")
                .description("AI model request duration")
                .publishPercentileHistogram()
                .tags(
                        "operation", safe(operation),
                        "task", safe(route.taskType()),
                        "model", safe(route.model()),
                        "outcome", outcome
                )
                .register(meterRegistry)
                .record(Math.max(0L, System.nanoTime() - startedAtNanos), TimeUnit.NANOSECONDS);
    }

    private void incrementTokens(AiTaskRouter.ModelRoute route, String tokenType, Integer value) {
        if (value == null || value <= 0) {
            return;
        }
        Counter.builder("matrix.ai.model.tokens")
                .description("AI model token usage")
                .tags(
                        "task", safe(route.taskType()),
                        "model", safe(route.model()),
                        "type", tokenType
                )
                .register(meterRegistry)
                .increment(value.doubleValue());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
