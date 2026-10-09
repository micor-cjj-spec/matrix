package single.cjj.scheduler.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;
import single.cjj.scheduler.entity.MatrixSchedulerAlertRecord;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.entity.MatrixSchedulerExecutor;
import single.cjj.scheduler.entity.MatrixSchedulerOutbox;
import single.cjj.scheduler.mapper.MatrixSchedulerAlertRecordMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutionMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutorMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerOutboxMapper;

@Component
public class SchedulerMetricsBinder implements MeterBinder {

    private final MatrixSchedulerExecutionMapper executionMapper;
    private final MatrixSchedulerExecutorMapper executorMapper;
    private final MatrixSchedulerOutboxMapper outboxMapper;
    private final MatrixSchedulerAlertRecordMapper alertMapper;

    public SchedulerMetricsBinder(MatrixSchedulerExecutionMapper executionMapper,
                                  MatrixSchedulerExecutorMapper executorMapper,
                                  MatrixSchedulerOutboxMapper outboxMapper,
                                  MatrixSchedulerAlertRecordMapper alertMapper) {
        this.executionMapper = executionMapper;
        this.executorMapper = executorMapper;
        this.outboxMapper = outboxMapper;
        this.alertMapper = alertMapper;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("matrix_scheduler_execution_active", this, value -> value.countActiveExecutions())
                .description("Current active scheduler executions")
                .register(registry);
        Gauge.builder("matrix_scheduler_execution_waiting", this, value -> value.countWaitingExecutions())
                .description("Current waiting scheduler executions")
                .register(registry);
        Gauge.builder("matrix_scheduler_execution_retry_wait", this, value -> value.countRetryWaitExecutions())
                .description("Current executions waiting for retry")
                .register(registry);
        Gauge.builder("matrix_scheduler_executor_online", this, value -> value.countOnlineExecutors())
                .description("Online scheduler executors")
                .register(registry);
        Gauge.builder("matrix_scheduler_outbox_pending", this, value -> value.countPendingOutbox())
                .description("Pending scheduler outbox events")
                .register(registry);
        Gauge.builder("matrix_scheduler_alert_pending", this, value -> value.countPendingAlerts())
                .description("Pending scheduler alerts")
                .register(registry);
    }

    private double countActiveExecutions() {
        return safe(executionMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerExecution>()
                .in(MatrixSchedulerExecution::getFstatus, "CREATED", "QUEUED", "RUNNING")));
    }

    private double countWaitingExecutions() {
        return safe(executionMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerExecution>()
                .in(MatrixSchedulerExecution::getFstatus, "WAITING", "WAITING_RESOURCE")));
    }

    private double countRetryWaitExecutions() {
        return safe(executionMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerExecution>()
                .eq(MatrixSchedulerExecution::getFstatus, "RETRY_WAIT")));
    }

    private double countOnlineExecutors() {
        return safe(executorMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerExecutor>()
                .eq(MatrixSchedulerExecutor::getFstatus, "ONLINE")));
    }

    private double countPendingOutbox() {
        return safe(outboxMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerOutbox>()
                .in(MatrixSchedulerOutbox::getFstatus, "PENDING", "FAILED")));
    }

    private double countPendingAlerts() {
        return safe(alertMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerAlertRecord>()
                .eq(MatrixSchedulerAlertRecord::getFstatus, "PENDING")));
    }

    private double safe(Long value) {
        return value == null ? 0D : value.doubleValue();
    }
}
