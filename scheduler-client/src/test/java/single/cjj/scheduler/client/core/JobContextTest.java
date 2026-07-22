package single.cjj.scheduler.client.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobContextTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReadTypedParameters() {
        JobContext context = new JobContext(
                "EXEC-1",
                "TRACE-1",
                "JOB-1",
                "database-health-check",
                2,
                "{\"bookId\":10001,\"period\":\"2026-07\"}",
                objectMapper);

        assertEquals(10001L, context.getLong("bookId"));
        assertEquals("2026-07", context.getString("period"));
        assertEquals(2, context.getAttemptNo());
    }

    @Test
    void shouldRejectInvalidJson() {
        assertThrows(IllegalArgumentException.class, () -> new JobContext(
                "EXEC-1", "TRACE-1", "JOB-1", "handler", 1, "not-json", objectMapper));
    }
}
