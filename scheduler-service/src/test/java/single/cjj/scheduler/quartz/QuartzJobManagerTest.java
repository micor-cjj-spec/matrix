package single.cjj.scheduler.quartz;

import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class QuartzJobManagerTest {

    @Test
    void shouldPreviewNextFireTimesWithTimezone() {
        QuartzJobManager manager = new QuartzJobManager(mock(Scheduler.class));

        List<LocalDateTime> times = manager.preview(
                "0 0 2 * * ?",
                "Asia/Shanghai",
                5
        );

        assertEquals(5, times.size());
        for (int i = 1; i < times.size(); i++) {
            assertTrue(times.get(i).isAfter(times.get(i - 1)));
        }
    }
}
