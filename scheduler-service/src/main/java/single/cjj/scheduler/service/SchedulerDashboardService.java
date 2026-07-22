package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import single.cjj.scheduler.entity.MatrixSchedulerAlertRecord;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.entity.MatrixSchedulerExecutor;
import single.cjj.scheduler.entity.MatrixSchedulerOutbox;
import single.cjj.scheduler.mapper.MatrixSchedulerAlertRecordMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutionMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutorMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerOutboxMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchedulerDashboardService {

    private final MatrixSchedulerExecutionMapper executionMapper;
    private final MatrixSchedulerExecutorMapper executorMapper;
    private final MatrixSchedulerOutboxMapper outboxMapper;
    private final MatrixSchedulerAlertRecordMapper alertMapper;

    public SchedulerDashboardService(MatrixSchedulerExecutionMapper executionMapper,
                                     MatrixSchedulerExecutorMapper executorMapper,
                                     MatrixSchedulerOutboxMapper outboxMapper,
                                     MatrixSchedulerAlertRecordMapper alertMapper) {
        this.executionMapper = executionMapper;
        this.executorMapper = executorMapper;
        this.outboxMapper = outboxMapper;
        this.alertMapper = alertMapper;
    }

    public Map<String, Object> summary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long total = countExecutions(startOfDay, null);
        long success = countExecutions(startOfDay, List.of("SUCCESS"));
        long failed = countExecutions(startOfDay, List.of("FAILED", "DEAD"));
        long timeout = countExecutions(startOfDay, List.of("TIMEOUT"));
        long waiting = countAllByStatus(List.of("WAITING", "WAITING_RESOURCE"));
        long retryWait = countAllByStatus(List.of("RETRY_WAIT"));
        long running = countAllByStatus(List.of("CREATED", "QUEUED", "RUNNING"));
        long onlineExecutors = countExecutors("ONLINE");
        long offlineExecutors = countExecutors("OFFLINE");
        long pendingOutbox = countOutbox(List.of("PENDING", "FAILED"));
        long deadOutbox = countOutbox(List.of("DEAD"));
        long pendingAlerts = countAlerts("PENDING");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayExecutionTotal", total);
        result.put("todaySuccess", success);
        result.put("todayFailed", failed);
        result.put("todayTimeout", timeout);
        result.put("todaySuccessRate", total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(success * 100D / total).setScale(2, RoundingMode.HALF_UP));
        result.put("activeExecutions", running);
        result.put("waitingExecutions", waiting);
        result.put("retryWaitExecutions", retryWait);
        result.put("onlineExecutors", onlineExecutors);
        result.put("offlineExecutors", offlineExecutors);
        result.put("pendingOutbox", pendingOutbox);
        result.put("deadOutbox", deadOutbox);
        result.put("pendingAlerts", pendingAlerts);
        result.put("recentFailures", executionMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .in(MatrixSchedulerExecution::getFstatus, "FAILED", "TIMEOUT", "DEAD")
                        .orderByDesc(MatrixSchedulerExecution::getFupdateTime)
                        .last("LIMIT 10")));
        return result;
    }

    private long countExecutions(LocalDateTime start, List<String> statuses) {
        LambdaQueryWrapper<MatrixSchedulerExecution> wrapper = new LambdaQueryWrapper<MatrixSchedulerExecution>()
                .ge(MatrixSchedulerExecution::getFcreateTime, start);
        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(MatrixSchedulerExecution::getFstatus, statuses);
        }
        return safe(executionMapper.selectCount(wrapper));
    }

    private long countAllByStatus(List<String> statuses) {
        return safe(executionMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerExecution>()
                .in(MatrixSchedulerExecution::getFstatus, statuses)));
    }

    private long countExecutors(String status) {
        return safe(executorMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerExecutor>()
                .eq(MatrixSchedulerExecutor::getFstatus, status)));
    }

    private long countOutbox(List<String> statuses) {
        return safe(outboxMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerOutbox>()
                .in(MatrixSchedulerOutbox::getFstatus, statuses)));
    }

    private long countAlerts(String status) {
        return safe(alertMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerAlertRecord>()
                .eq(MatrixSchedulerAlertRecord::getFstatus, status)));
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }
}
