package single.cjj.matrix.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AiToolMetrics {

    private final MeterRegistry meterRegistry;

    public AiToolMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public long start() {
        return System.nanoTime();
    }

    public void record(String tool, String outcome, long startedAtNanos) {
        Counter.builder("matrix.ai.tools.calls")
                .description("Controlled AI tool calls")
                .tags("tool", safe(tool), "outcome", safe(outcome))
                .register(meterRegistry)
                .increment();
        Timer.builder("matrix.ai.tools.duration")
                .description("Controlled AI tool call duration")
                .publishPercentileHistogram()
                .tags("tool", safe(tool), "outcome", safe(outcome))
                .register(meterRegistry)
                .record(Math.max(0L, System.nanoTime() - startedAtNanos), TimeUnit.NANOSECONDS);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
