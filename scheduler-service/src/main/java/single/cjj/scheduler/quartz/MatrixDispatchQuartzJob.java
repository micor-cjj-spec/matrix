package single.cjj.scheduler.quartz;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import single.cjj.scheduler.service.SchedulerDispatchService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@DisallowConcurrentExecution
public class MatrixDispatchQuartzJob implements Job {

    @Autowired
    private SchedulerDispatchService dispatchService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Long jobId = context.getMergedJobDataMap().getLong("jobId");
            String triggerType = context.getMergedJobDataMap().getString("triggerType");
            if (triggerType == null || triggerType.isBlank()) {
                triggerType = "CRON";
            }
            Date fireTime = context.getScheduledFireTime() == null
                    ? new Date()
                    : context.getScheduledFireTime();
            LocalDateTime scheduledTime = LocalDateTime.ofInstant(
                    fireTime.toInstant(), ZoneId.systemDefault());
            dispatchService.createExecution(jobId, scheduledTime, triggerType);
        } catch (Exception e) {
            throw new JobExecutionException("创建调度执行实例失败", e, false);
        }
    }
}
