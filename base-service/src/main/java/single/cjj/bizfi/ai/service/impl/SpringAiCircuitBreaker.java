package single.cjj.bizfi.ai.service.impl;

import org.springframework.stereotype.Component;
import single.cjj.bizfi.ai.config.AiProperties;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Small in-process circuit breaker for the independent ai-service client.
 */
@Component
public class SpringAiCircuitBreaker {

    private final AiProperties properties;
    private final LongSupplier currentTimeMillis;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong openUntilMillis = new AtomicLong();

    public SpringAiCircuitBreaker(AiProperties properties) {
        this(properties, System::currentTimeMillis);
    }

    SpringAiCircuitBreaker(AiProperties properties, LongSupplier currentTimeMillis) {
        this.properties = properties;
        this.currentTimeMillis = currentTimeMillis;
    }

    public void acquirePermission() {
        long openUntil = openUntilMillis.get();
        if (openUntil <= 0) {
            return;
        }
        long now = currentTimeMillis.getAsLong();
        if (now < openUntil) {
            throw new IllegalStateException("ai-service 熔断器已打开");
        }
        openUntilMillis.compareAndSet(openUntil, 0L);
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);
        openUntilMillis.set(0L);
    }

    public boolean recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        int threshold = positive(properties.getSpringAiCircuitFailureThreshold(), 3);
        if (failures < threshold) {
            return false;
        }
        long waitMillis = positive(properties.getSpringAiCircuitOpenSeconds(), 30) * 1000L;
        long openUntil = currentTimeMillis.getAsLong() + waitMillis;
        long previous = openUntilMillis.getAndSet(openUntil);
        return previous <= currentTimeMillis.getAsLong();
    }

    int consecutiveFailures() {
        return consecutiveFailures.get();
    }

    boolean isOpen() {
        return openUntilMillis.get() > currentTimeMillis.getAsLong();
    }

    private int positive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }
}
