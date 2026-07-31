package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.ai.config.AiProperties;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAiCircuitBreakerTest {

    @Test
    void shouldOpenAfterConfiguredFailuresAndRecoverAfterWait() {
        AiProperties properties = new AiProperties();
        properties.setSpringAiCircuitFailureThreshold(2);
        properties.setSpringAiCircuitOpenSeconds(10);
        AtomicLong now = new AtomicLong(1_000L);
        SpringAiCircuitBreaker breaker = new SpringAiCircuitBreaker(properties, now::get);

        breaker.recordFailure();
        assertEquals(1, breaker.consecutiveFailures());
        assertFalse(breaker.isOpen());

        breaker.recordFailure();
        assertTrue(breaker.isOpen());
        assertThrows(IllegalStateException.class, breaker::acquirePermission);

        now.addAndGet(10_000L);
        assertDoesNotThrow(breaker::acquirePermission);
        breaker.recordSuccess();

        assertFalse(breaker.isOpen());
        assertEquals(0, breaker.consecutiveFailures());
    }
}
