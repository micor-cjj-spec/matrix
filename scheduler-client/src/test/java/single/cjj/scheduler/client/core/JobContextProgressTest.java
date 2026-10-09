package single.cjj.scheduler.client.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobContextProgressTest {

    @Test
    void shouldReportProgressThroughCallback() {
        AtomicInteger progress = new AtomicInteger();
        AtomicReference<String> stage = new AtomicReference<>();
        AtomicReference<String> message = new AtomicReference<>();
        JobContext context = new JobContext(
                "EXEC-1", "TRACE-1", "JOB-1", "handler", 1,
                "{\"period\":\"2026-07\"}", new ObjectMapper(),
                (value, currentStage, currentMessage) -> {
                    progress.set(value);
                    stage.set(currentStage);
                    message.set(currentMessage);
                });

        context.reportProgress(60, "AGGREGATING", "正在汇总");

        assertEquals(60, progress.get());
        assertEquals("AGGREGATING", stage.get());
        assertEquals("正在汇总", message.get());
        assertEquals("2026-07", context.getRequiredString("period"));
    }

    @Test
    void shouldRejectInvalidProgress() {
        JobContext context = new JobContext(
                "EXEC-1", "TRACE-1", "JOB-1", "handler", 1,
                "{}", new ObjectMapper());

        assertThrows(IllegalArgumentException.class,
                () -> context.reportProgress(101, "INVALID", "invalid"));
    }
}
