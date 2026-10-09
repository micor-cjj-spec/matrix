package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.entity.MatrixSchedulerJob;
import single.cjj.scheduler.entity.MatrixSchedulerOutbox;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutionMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerJobMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerOutboxMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SchedulerSerialQueueScanner {

    private final MatrixSchedulerExecutionMapper executionMapper;
    private final MatrixSchedulerJobMapper jobMapper;
    private final MatrixSchedulerOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    public SchedulerSerialQueueScanner(MatrixSchedulerExecutionMapper executionMapper,
                                       MatrixSchedulerJobMapper jobMapper,
                                       MatrixSchedulerOutboxMapper outboxMapper,
                                       ObjectMapper objectMapper,
                                       @Value("${matrix.scheduler.reliability.batch-size:100}") int batchSize) {
        this.executionMapper = executionMapper;
        this.jobMapper = jobMapper;
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
    }

    @Scheduled(fixedDelayString = "${matrix.scheduler.reliability.serial-scan-ms:3000}")
    @Transactional(rollbackFor = Exception.class)
    public void promoteWaitingExecutions() {
        List<MatrixSchedulerExecution> waitingList = executionMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .eq(MatrixSchedulerExecution::getFstatus, "WAITING")
                        .orderByAsc(MatrixSchedulerExecution::getFscheduledTime)
                        .last("LIMIT " + batchSize));
        for (MatrixSchedulerExecution waiting : waitingList) {
            Long active = executionMapper.selectCount(
                    new LambdaQueryWrapper<MatrixSchedulerExecution>()
                            .eq(MatrixSchedulerExecution::getFjobId, waiting.getFjobId())
                            .in(MatrixSchedulerExecution::getFstatus,
                                    "CREATED", "QUEUED", "RUNNING", "RETRY_WAIT"));
            if (active != null && active > 0) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            int updated = executionMapper.update(null,
                    new LambdaUpdateWrapper<MatrixSchedulerExecution>()
                            .eq(MatrixSchedulerExecution::getFid, waiting.getFid())
                            .eq(MatrixSchedulerExecution::getFstatus, "WAITING")
                            .set(MatrixSchedulerExecution::getFstatus, "CREATED")
                            .set(MatrixSchedulerExecution::getFupdateTime, now));
            if (updated != 1) {
                continue;
            }

            MatrixSchedulerJob job = jobMapper.selectById(waiting.getFjobId());
            if (job == null || "DELETED".equals(job.getFstatus())) {
                continue;
            }
            waiting.setFstatus("CREATED");
            createOutbox(job, waiting, now);
        }
    }

    private void createOutbox(MatrixSchedulerJob job,
                              MatrixSchedulerExecution execution,
                              LocalDateTime now) {
        MatrixSchedulerOutbox outbox = new MatrixSchedulerOutbox();
        outbox.setFid(IdWorker.getId());
        outbox.setFeventId(UUID.randomUUID().toString());
        outbox.setFeventType("SCHEDULER_EXECUTE");
        outbox.setFaggregateId(execution.getFexecutionNo());
        outbox.setFroutingKey("scheduler.execute." + job.getFexecutorCode());
        outbox.setFpayload(buildPayload(job, execution));
        outbox.setFstatus("PENDING");
        outbox.setFretryCount(0);
        outbox.setFcreateTime(now);
        outbox.setFupdateTime(now);
        outboxMapper.insert(outbox);
    }

    private String buildPayload(MatrixSchedulerJob job,
                                MatrixSchedulerExecution execution) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("executionNo", execution.getFexecutionNo());
        payload.put("traceId", execution.getFtraceId());
        payload.put("jobId", job.getFid());
        payload.put("jobCode", job.getFjobCode());
        payload.put("tenantId", job.getFtenantId());
        payload.put("executorCode", job.getFexecutorCode());
        payload.put("handlerCode", job.getFhandlerCode());
        payload.put("executeType", job.getFexecuteType());
        payload.put("attemptNo", execution.getFattemptNo());
        payload.put("timeoutSeconds", job.getFtimeoutSeconds());
        payload.put("parameters", job.getFexecuteParameters());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("串行任务消息序列化失败", e);
        }
    }
}
