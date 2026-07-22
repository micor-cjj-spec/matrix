package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutionMapper;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SchedulerReliabilityScanner {

    private final MatrixSchedulerExecutionMapper executionMapper;
    private final SchedulerDispatchService dispatchService;
    private final int batchSize;
    private final long dispatchTimeoutSeconds;

    public SchedulerReliabilityScanner(MatrixSchedulerExecutionMapper executionMapper,
                                       SchedulerDispatchService dispatchService,
                                       @Value("${matrix.scheduler.reliability.batch-size:100}") int batchSize,
                                       @Value("${matrix.scheduler.reliability.dispatch-timeout-seconds:120}") long dispatchTimeoutSeconds) {
        this.executionMapper = executionMapper;
        this.dispatchService = dispatchService;
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
        this.dispatchTimeoutSeconds = Math.max(10, dispatchTimeoutSeconds);
    }

    @Scheduled(fixedDelayString = "${matrix.scheduler.reliability.retry-scan-ms:5000}")
    public void retryDueExecutions() {
        List<MatrixSchedulerExecution> due = executionMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .eq(MatrixSchedulerExecution::getFstatus, "RETRY_WAIT")
                        .le(MatrixSchedulerExecution::getFnextRetryTime, LocalDateTime.now())
                        .orderByAsc(MatrixSchedulerExecution::getFnextRetryTime)
                        .last("LIMIT " + batchSize));
        for (MatrixSchedulerExecution execution : due) {
            dispatchService.retry(execution.getFexecutionNo());
        }
    }

    @Scheduled(fixedDelayString = "${matrix.scheduler.reliability.timeout-scan-ms:10000}")
    public void timeoutStaleExecutions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dispatchDeadline = now.minusSeconds(dispatchTimeoutSeconds);

        List<MatrixSchedulerExecution> dispatchTimeouts = executionMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .in(MatrixSchedulerExecution::getFstatus, "CREATED", "QUEUED")
                        .lt(MatrixSchedulerExecution::getFcreateTime, dispatchDeadline)
                        .orderByAsc(MatrixSchedulerExecution::getFcreateTime)
                        .last("LIMIT " + batchSize));
        for (MatrixSchedulerExecution execution : dispatchTimeouts) {
            dispatchService.timeout(execution,
                    "DISPATCH_TIMEOUT",
                    "任务投递后未在规定时间内开始执行");
        }

        List<MatrixSchedulerExecution> runningTimeouts = executionMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .eq(MatrixSchedulerExecution::getFstatus, "RUNNING")
                        .isNotNull(MatrixSchedulerExecution::getFdeadlineTime)
                        .le(MatrixSchedulerExecution::getFdeadlineTime, now)
                        .orderByAsc(MatrixSchedulerExecution::getFdeadlineTime)
                        .last("LIMIT " + batchSize));
        for (MatrixSchedulerExecution execution : runningTimeouts) {
            dispatchService.timeout(execution,
                    "EXECUTION_TIMEOUT",
                    "任务执行超过配置的 timeoutSeconds");
        }
    }
}
