package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.scheduler.dto.ExecutionCallbackRequest;
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

@Service
public class SchedulerDispatchService {

    private final MatrixSchedulerJobMapper jobMapper;
    private final MatrixSchedulerExecutionMapper executionMapper;
    private final MatrixSchedulerOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    public SchedulerDispatchService(MatrixSchedulerJobMapper jobMapper,
                                    MatrixSchedulerExecutionMapper executionMapper,
                                    MatrixSchedulerOutboxMapper outboxMapper,
                                    ObjectMapper objectMapper) {
        this.jobMapper = jobMapper;
        this.executionMapper = executionMapper;
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerExecution createExecution(Long jobId,
                                                    LocalDateTime scheduledTime,
                                                    String triggerType) {
        MatrixSchedulerJob job = jobMapper.selectById(jobId);
        if (job == null || "DELETED".equals(job.getFstatus())) {
            throw new IllegalArgumentException("调度任务不存在: " + jobId);
        }
        if ("CRON".equals(triggerType) && !"ENABLED".equals(job.getFstatus())) {
            return createSkipped(job, scheduledTime, triggerType, "JOB_NOT_ENABLED");
        }

        String idempotencyKey = "CRON".equals(triggerType)
                ? jobId + ":" + scheduledTime + ":CRON"
                : jobId + ":MANUAL:" + UUID.randomUUID();
        MatrixSchedulerExecution existing = executionMapper.selectOne(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .eq(MatrixSchedulerExecution::getFidempotencyKey, idempotencyKey)
                        .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }

        if (!"PARALLEL".equals(job.getFconcurrencyPolicy()) && hasRunningExecution(jobId)) {
            return createSkipped(job, scheduledTime, triggerType, "PREVIOUS_EXECUTION_RUNNING");
        }

        LocalDateTime now = LocalDateTime.now();
        MatrixSchedulerExecution execution = new MatrixSchedulerExecution();
        execution.setFid(IdWorker.getId());
        execution.setFexecutionNo("EXEC-" + UUID.randomUUID().toString().replace("-", ""));
        execution.setFjobId(jobId);
        execution.setFjobCode(job.getFjobCode());
        execution.setFscheduledTime(scheduledTime);
        execution.setFtriggerType(triggerType);
        execution.setFstatus("CREATED");
        execution.setFattemptNo(1);
        execution.setFexecutorCode(job.getFexecutorCode());
        execution.setFhandlerCode(job.getFhandlerCode());
        execution.setFrequestPayload(job.getFexecuteParameters());
        execution.setFtraceId(UUID.randomUUID().toString().replace("-", ""));
        execution.setFidempotencyKey(idempotencyKey);
        execution.setFcreateTime(now);
        execution.setFupdateTime(now);
        executionMapper.insert(execution);

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

        job.setFlastFireTime(now);
        job.setFupdateTime(now);
        jobMapper.updateById(job);
        return execution;
    }

    public IPage<MatrixSchedulerExecution> listExecutions(int page, int size,
                                                           Long jobId, String status) {
        LambdaQueryWrapper<MatrixSchedulerExecution> wrapper =
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .eq(jobId != null, MatrixSchedulerExecution::getFjobId, jobId)
                        .eq(StringUtils.hasText(status), MatrixSchedulerExecution::getFstatus, status)
                        .orderByDesc(MatrixSchedulerExecution::getFcreateTime);
        return executionMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public MatrixSchedulerExecution getExecution(String executionNo) {
        MatrixSchedulerExecution execution = executionMapper.selectOne(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .eq(MatrixSchedulerExecution::getFexecutionNo, executionNo)
                        .last("LIMIT 1"));
        if (execution == null) {
            throw new IllegalArgumentException("执行实例不存在");
        }
        return execution;
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerExecution callback(String executionNo,
                                             ExecutionCallbackRequest request) {
        MatrixSchedulerExecution execution = getExecution(executionNo);
        String targetStatus = request.getStatus().toUpperCase();
        if (!List.of("RUNNING", "SUCCESS", "FAILED", "TIMEOUT", "CANCELLED").contains(targetStatus)) {
            throw new IllegalArgumentException("不支持的执行状态: " + targetStatus);
        }
        if (isTerminal(execution.getFstatus())) {
            return execution;
        }

        LocalDateTime now = LocalDateTime.now();
        execution.setFstatus(targetStatus);
        execution.setFexecutorInstance(request.getExecutorInstance());
        execution.setFresponsePayload(request.getResponsePayload());
        execution.setFerrorCode(request.getErrorCode());
        execution.setFerrorMessage(trimError(request.getErrorMessage()));
        if ("RUNNING".equals(targetStatus) && execution.getFactualStartTime() == null) {
            execution.setFactualStartTime(now);
        }
        if (isTerminal(targetStatus)) {
            if (execution.getFactualStartTime() == null) {
                execution.setFactualStartTime(now);
            }
            execution.setFactualEndTime(now);
        }
        execution.setFupdateTime(now);
        executionMapper.updateById(execution);
        return execution;
    }

    private MatrixSchedulerExecution createSkipped(MatrixSchedulerJob job,
                                                    LocalDateTime scheduledTime,
                                                    String triggerType,
                                                    String reason) {
        LocalDateTime now = LocalDateTime.now();
        MatrixSchedulerExecution skipped = new MatrixSchedulerExecution();
        skipped.setFid(IdWorker.getId());
        skipped.setFexecutionNo("EXEC-" + UUID.randomUUID().toString().replace("-", ""));
        skipped.setFjobId(job.getFid());
        skipped.setFjobCode(job.getFjobCode());
        skipped.setFscheduledTime(scheduledTime);
        skipped.setFtriggerType(triggerType);
        skipped.setFstatus("SKIPPED");
        skipped.setFattemptNo(1);
        skipped.setFexecutorCode(job.getFexecutorCode());
        skipped.setFhandlerCode(job.getFhandlerCode());
        skipped.setFerrorCode(reason);
        skipped.setFerrorMessage(reason);
        skipped.setFidempotencyKey(job.getFid() + ":SKIPPED:" + scheduledTime + ":" + reason);
        skipped.setFcreateTime(now);
        skipped.setFupdateTime(now);
        executionMapper.insert(skipped);
        return skipped;
    }

    private boolean hasRunningExecution(Long jobId) {
        Long count = executionMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerExecution>()
                .eq(MatrixSchedulerExecution::getFjobId, jobId)
                .in(MatrixSchedulerExecution::getFstatus, "CREATED", "QUEUED", "RUNNING", "RETRY_WAIT"));
        return count != null && count > 0;
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
        payload.put("timeoutSeconds", job.getFtimeoutSeconds());
        payload.put("retryCount", job.getFretryCount());
        payload.put("parameters", job.getFexecuteParameters());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("调度消息序列化失败", e);
        }
    }

    private boolean isTerminal(String status) {
        return List.of("SUCCESS", "FAILED", "TIMEOUT", "CANCELLED", "DEAD", "SKIPPED")
                .contains(status);
    }

    private String trimError(String message) {
        if (message == null || message.length() <= 2000) {
            return message;
        }
        return message.substring(0, 2000);
    }
}
