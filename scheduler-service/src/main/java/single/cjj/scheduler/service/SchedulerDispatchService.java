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
        MatrixSchedulerJob job = requiredJob(jobId);
        if ("CRON".equals(triggerType) && !"ENABLED".equals(job.getFstatus())) {
            return createSkipped(job, scheduledTime, triggerType, "JOB_NOT_ENABLED");
        }

        String idempotencyKey = "CRON".equals(triggerType)
                ? jobId + ":" + scheduledTime + ":CRON"
                : jobId + ":MANUAL:" + UUID.randomUUID();
        MatrixSchedulerExecution existing = findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }

        boolean active = hasActiveExecution(jobId);
        if (active && "SKIP".equals(job.getFconcurrencyPolicy())) {
            return createSkipped(job, scheduledTime, triggerType, "PREVIOUS_EXECUTION_RUNNING");
        }
        if (active && "SERIAL".equals(job.getFconcurrencyPolicy())) {
            return createExecutionRecord(job, scheduledTime, triggerType,
                    "WAITING", 1, null, null, idempotencyKey, false);
        }

        MatrixSchedulerExecution execution = createExecutionRecord(job, scheduledTime, triggerType,
                "CREATED", 1, null, null, idempotencyKey, true);
        LocalDateTime now = LocalDateTime.now();
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

        MatrixSchedulerJob job = requiredJob(execution.getFjobId());
        LocalDateTime now = LocalDateTime.now();
        execution.setFexecutorInstance(request.getExecutorInstance());
        execution.setFresponsePayload(request.getResponsePayload());
        execution.setFerrorCode(request.getErrorCode());
        execution.setFerrorMessage(trimError(request.getErrorMessage()));

        if ("RUNNING".equals(targetStatus)) {
            execution.setFstatus("RUNNING");
            if (execution.getFactualStartTime() == null) {
                execution.setFactualStartTime(now);
            }
            execution.setFdeadlineTime(now.plusSeconds(Math.max(1, job.getFtimeoutSeconds())));
        } else if ("SUCCESS".equals(targetStatus)) {
            finish(execution, "SUCCESS", now);
            wakeNextSerial(job);
        } else if ("FAILED".equals(targetStatus) || "TIMEOUT".equals(targetStatus)) {
            if (canRetry(execution, job)) {
                execution.setFstatus("RETRY_WAIT");
                execution.setFactualEndTime(now);
                execution.setFnextRetryTime(now.plusSeconds(retryDelaySeconds(execution, job)));
                if ("TIMEOUT".equals(targetStatus) && !StringUtils.hasText(execution.getFerrorCode())) {
                    execution.setFerrorCode("EXECUTION_TIMEOUT");
                }
            } else {
                finish(execution, targetStatus, now);
                wakeNextSerial(job);
            }
        } else {
            finish(execution, "CANCELLED", now);
            wakeNextSerial(job);
        }

        execution.setFupdateTime(now);
        executionMapper.updateById(execution);
        return execution;
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerExecution retry(String executionNo) {
        MatrixSchedulerExecution parent = getExecution(executionNo);
        if (!"RETRY_WAIT".equals(parent.getFstatus())) {
            return parent;
        }
        if (parent.getFnextRetryTime() != null && parent.getFnextRetryTime().isAfter(LocalDateTime.now())) {
            return parent;
        }

        MatrixSchedulerJob job = requiredJob(parent.getFjobId());
        if (!canRetry(parent, job)) {
            finish(parent, terminalFailureStatus(parent), LocalDateTime.now());
            executionMapper.updateById(parent);
            wakeNextSerial(job);
            return parent;
        }

        int nextAttempt = parent.getFattemptNo() + 1;
        Long rootId = parent.getFrootExecutionId() == null ? parent.getFid() : parent.getFrootExecutionId();
        String idempotencyKey = rootId + ":RETRY:" + nextAttempt;
        MatrixSchedulerExecution existing = findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }

        parent.setFstatus(terminalFailureStatus(parent));
        parent.setFnextRetryTime(null);
        parent.setFupdateTime(LocalDateTime.now());
        executionMapper.updateById(parent);

        return createExecutionRecord(job, LocalDateTime.now(), "RETRY",
                "CREATED", nextAttempt, rootId, parent.getFid(), idempotencyKey, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void timeout(MatrixSchedulerExecution execution, String errorCode, String errorMessage) {
        MatrixSchedulerExecution current = executionMapper.selectById(execution.getFid());
        if (current == null || !List.of("CREATED", "QUEUED", "RUNNING").contains(current.getFstatus())) {
            return;
        }
        MatrixSchedulerJob job = requiredJob(current.getFjobId());
        LocalDateTime now = LocalDateTime.now();
        current.setFerrorCode(errorCode);
        current.setFerrorMessage(trimError(errorMessage));
        current.setFactualEndTime(now);
        if (canRetry(current, job)) {
            current.setFstatus("RETRY_WAIT");
            current.setFnextRetryTime(now.plusSeconds(retryDelaySeconds(current, job)));
        } else {
            current.setFstatus("TIMEOUT");
            wakeNextSerial(job);
        }
        current.setFupdateTime(now);
        executionMapper.updateById(current);
    }

    private MatrixSchedulerExecution createExecutionRecord(MatrixSchedulerJob job,
                                                             LocalDateTime scheduledTime,
                                                             String triggerType,
                                                             String status,
                                                             int attemptNo,
                                                             Long rootExecutionId,
                                                             Long parentExecutionId,
                                                             String idempotencyKey,
                                                             boolean createOutbox) {
        LocalDateTime now = LocalDateTime.now();
        MatrixSchedulerExecution execution = new MatrixSchedulerExecution();
        execution.setFid(IdWorker.getId());
        execution.setFexecutionNo("EXEC-" + UUID.randomUUID().toString().replace("-", ""));
        execution.setFjobId(job.getFid());
        execution.setFjobCode(job.getFjobCode());
        execution.setFscheduledTime(scheduledTime);
        execution.setFtriggerType(triggerType);
        execution.setFstatus(status);
        execution.setFattemptNo(attemptNo);
        execution.setFrootExecutionId(rootExecutionId == null ? execution.getFid() : rootExecutionId);
        execution.setFparentExecutionId(parentExecutionId);
        execution.setFexecutorCode(job.getFexecutorCode());
        execution.setFhandlerCode(job.getFhandlerCode());
        execution.setFrequestPayload(job.getFexecuteParameters());
        execution.setFtraceId(UUID.randomUUID().toString().replace("-", ""));
        execution.setFidempotencyKey(idempotencyKey);
        execution.setFcreateTime(now);
        execution.setFupdateTime(now);
        executionMapper.insert(execution);
        if (createOutbox) {
            createOutbox(job, execution, now);
        }
        return execution;
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

    private MatrixSchedulerExecution createSkipped(MatrixSchedulerJob job,
                                                     LocalDateTime scheduledTime,
                                                     String triggerType,
                                                     String reason) {
        MatrixSchedulerExecution skipped = createExecutionRecord(job, scheduledTime, triggerType,
                "SKIPPED", 1, null, null,
                job.getFid() + ":SKIPPED:" + scheduledTime + ":" + reason + ":" + UUID.randomUUID(), false);
        skipped.setFerrorCode(reason);
        skipped.setFerrorMessage(reason);
        skipped.setFactualEndTime(LocalDateTime.now());
        skipped.setFupdateTime(LocalDateTime.now());
        executionMapper.updateById(skipped);
        return skipped;
    }

    private void wakeNextSerial(MatrixSchedulerJob job) {
        if (!"SERIAL".equals(job.getFconcurrencyPolicy()) || hasActiveExecution(job.getFid())) {
            return;
        }
        MatrixSchedulerExecution waiting = executionMapper.selectOne(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .eq(MatrixSchedulerExecution::getFjobId, job.getFid())
                        .eq(MatrixSchedulerExecution::getFstatus, "WAITING")
                        .orderByAsc(MatrixSchedulerExecution::getFscheduledTime)
                        .last("LIMIT 1"));
        if (waiting == null) {
            return;
        }
        waiting.setFstatus("CREATED");
        waiting.setFupdateTime(LocalDateTime.now());
        executionMapper.updateById(waiting);
        createOutbox(job, waiting, LocalDateTime.now());
    }

    private boolean hasActiveExecution(Long jobId) {
        Long count = executionMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerExecution>()
                .eq(MatrixSchedulerExecution::getFjobId, jobId)
                .in(MatrixSchedulerExecution::getFstatus, "CREATED", "QUEUED", "RUNNING", "RETRY_WAIT"));
        return count != null && count > 0;
    }

    private MatrixSchedulerExecution findByIdempotencyKey(String idempotencyKey) {
        return executionMapper.selectOne(new LambdaQueryWrapper<MatrixSchedulerExecution>()
                .eq(MatrixSchedulerExecution::getFidempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    private MatrixSchedulerJob requiredJob(Long jobId) {
        MatrixSchedulerJob job = jobMapper.selectById(jobId);
        if (job == null || "DELETED".equals(job.getFstatus())) {
            throw new IllegalArgumentException("调度任务不存在: " + jobId);
        }
        return job;
    }

    private boolean canRetry(MatrixSchedulerExecution execution, MatrixSchedulerJob job) {
        int configuredRetries = job.getFretryCount() == null ? 0 : job.getFretryCount();
        int attemptNo = execution.getFattemptNo() == null ? 1 : execution.getFattemptNo();
        return attemptNo <= configuredRetries;
    }

    private long retryDelaySeconds(MatrixSchedulerExecution execution, MatrixSchedulerJob job) {
        long base = Math.max(1, job.getFretryIntervalSeconds() == null ? 60 : job.getFretryIntervalSeconds());
        int exponent = Math.min(Math.max(0, execution.getFattemptNo() - 1), 6);
        return Math.min(3600L, base * (1L << exponent));
    }

    private String terminalFailureStatus(MatrixSchedulerExecution execution) {
        return execution.getFerrorCode() != null && execution.getFerrorCode().contains("TIMEOUT")
                ? "TIMEOUT" : "FAILED";
    }

    private void finish(MatrixSchedulerExecution execution, String status, LocalDateTime now) {
        execution.setFstatus(status);
        if (execution.getFactualStartTime() == null) {
            execution.setFactualStartTime(now);
        }
        execution.setFactualEndTime(now);
        execution.setFnextRetryTime(null);
        execution.setFdeadlineTime(null);
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
