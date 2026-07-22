package single.cjj.scheduler.quartz;

import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Component;
import single.cjj.scheduler.entity.MatrixSchedulerJob;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Component
public class QuartzJobManager {

    private static final String GROUP = "MATRIX_SCHEDULER";
    private final Scheduler scheduler;

    public QuartzJobManager(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void upsert(MatrixSchedulerJob job) {
        try {
            JobKey jobKey = jobKey(job.getFid());
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            JobDetail detail = JobBuilder.newJob(MatrixDispatchQuartzJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("jobId", job.getFid())
                    .storeDurably()
                    .build();

            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                    .cronSchedule(job.getFcronExpression())
                    .inTimeZone(TimeZone.getTimeZone(job.getFtimezone()));

            scheduleBuilder = switch (job.getFmisfirePolicy()) {
                case "DO_NOTHING" -> scheduleBuilder.withMisfireHandlingInstructionDoNothing();
                case "FIRE_ALL" -> scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                default -> scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
            };

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(job.getFid()))
                    .forJob(detail)
                    .withSchedule(scheduleBuilder)
                    .build();

            scheduler.scheduleJob(detail, trigger);
            if (!"ENABLED".equals(job.getFstatus())) {
                scheduler.pauseJob(jobKey);
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException("Quartz 任务注册失败: " + e.getMessage(), e);
        }
    }

    public void pause(Long jobId) {
        execute(() -> scheduler.pauseJob(jobKey(jobId)), "暂停");
    }

    public void resume(Long jobId) {
        execute(() -> scheduler.resumeJob(jobKey(jobId)), "恢复");
    }

    public void delete(Long jobId) {
        execute(() -> scheduler.deleteJob(jobKey(jobId)), "删除");
    }

    public void runNow(Long jobId) {
        execute(() -> scheduler.triggerJob(
                jobKey(jobId),
                new JobDataMap(Map.of("triggerType", "MANUAL"))
        ), "立即执行");
    }

    public List<LocalDateTime> preview(String cronExpression, String timezone, int count) {
        try {
            CronExpression cron = new CronExpression(cronExpression);
            cron.setTimeZone(TimeZone.getTimeZone(timezone));
            ZoneId zoneId = ZoneId.of(timezone);
            Date cursor = new Date();
            List<LocalDateTime> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                cursor = cron.getNextValidTimeAfter(cursor);
                if (cursor == null) {
                    break;
                }
                result.add(LocalDateTime.ofInstant(cursor.toInstant(), zoneId));
            }
            return result;
        } catch (ParseException | RuntimeException e) {
            throw new IllegalArgumentException("无效的 Cron 表达式或时区: " + e.getMessage(), e);
        }
    }

    public LocalDateTime nextFireTime(String cronExpression, String timezone) {
        List<LocalDateTime> times = preview(cronExpression, timezone, 1);
        return times.isEmpty() ? null : times.get(0);
    }

    private JobKey jobKey(Long jobId) {
        return JobKey.jobKey("job-" + jobId, GROUP);
    }

    private TriggerKey triggerKey(Long jobId) {
        return TriggerKey.triggerKey("trigger-" + jobId, GROUP);
    }

    private void execute(QuartzAction action, String operation) {
        try {
            action.run();
        } catch (SchedulerException e) {
            throw new IllegalStateException("Quartz 任务" + operation + "失败: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface QuartzAction {
        void run() throws SchedulerException;
    }
}
