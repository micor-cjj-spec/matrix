package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.entity.MatrixSchedulerOperationLog;
import single.cjj.scheduler.entity.MatrixSchedulerOutbox;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutionMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerOperationLogMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerOutboxMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchedulerManualOperationService {

    private final MatrixSchedulerExecutionMapper executionMapper;
    private final MatrixSchedulerOperationLogMapper operationLogMapper;
    private final MatrixSchedulerOutboxMapper outboxMapper;
    private final SchedulerDispatchService dispatchService;

    public SchedulerManualOperationService(MatrixSchedulerExecutionMapper executionMapper,
                                           MatrixSchedulerOperationLogMapper operationLogMapper,
                                           MatrixSchedulerOutboxMapper outboxMapper,
                                           SchedulerDispatchService dispatchService) {
        this.executionMapper = executionMapper;
        this.operationLogMapper = operationLogMapper;
        this.outboxMapper = outboxMapper;
        this.dispatchService = dispatchService;
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerExecution retryNow(String executionNo, String operatorId, String reason) {
        MatrixSchedulerExecution execution = requiredExecution(executionNo);
        String fromStatus = execution.getFstatus();
        MatrixSchedulerExecution result;
        if ("RETRY_WAIT".equals(fromStatus)) {
            int updated = executionMapper.update(null,
                    new LambdaUpdateWrapper<MatrixSchedulerExecution>()
                            .eq(MatrixSchedulerExecution::getFid, execution.getFid())
                            .eq(MatrixSchedulerExecution::getFstatus, "RETRY_WAIT")
                            .set(MatrixSchedulerExecution::getFnextRetryTime, LocalDateTime.now().minusSeconds(1))
                            .set(MatrixSchedulerExecution::getFupdateTime, LocalDateTime.now()));
            ensureUpdated(updated);
            result = dispatchService.retry(executionNo);
        } else if (List.of("FAILED", "TIMEOUT", "DEAD", "CANCELLED", "SKIPPED").contains(fromStatus)) {
            result = dispatchService.createExecution(execution.getFjobId(), LocalDateTime.now(), "MANUAL_RETRY");
        } else {
            throw new IllegalArgumentException("当前状态不允许立即重试: " + fromStatus);
        }
        saveLog(executionNo, "RETRY_NOW", operatorId, reason, fromStatus, "RETRY_CREATED");
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerExecution stopRetry(String executionNo, String operatorId, String reason) {
        MatrixSchedulerExecution execution = requiredExecution(executionNo);
        if (!"RETRY_WAIT".equals(execution.getFstatus())) {
            throw new IllegalArgumentException("只有RETRY_WAIT状态允许终止重试");
        }
        String target = execution.getFerrorCode() != null && execution.getFerrorCode().contains("TIMEOUT")
                ? "TIMEOUT" : "FAILED";
        transition(execution, target, true);
        cancelPendingOutbox(executionNo, "MANUAL_STOP_RETRY");
        saveLog(executionNo, "STOP_RETRY", operatorId, reason, "RETRY_WAIT", target);
        return requiredExecution(executionNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerExecution cancel(String executionNo, String operatorId, String reason) {
        MatrixSchedulerExecution execution = requiredExecution(executionNo);
        if (!List.of("WAITING", "CREATED", "QUEUED", "RUNNING", "RETRY_WAIT").contains(execution.getFstatus())) {
            throw new IllegalArgumentException("当前状态不允许取消: " + execution.getFstatus());
        }
        String from = execution.getFstatus();
        transition(execution, "CANCELLED", true);
        cancelPendingOutbox(executionNo, "MANUAL_CANCEL");
        saveLog(executionNo, "CANCEL", operatorId, reason, from, "CANCELLED");
        return requiredExecution(executionNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerExecution skip(String executionNo, String operatorId, String reason) {
        MatrixSchedulerExecution execution = requiredExecution(executionNo);
        if (!List.of("WAITING", "CREATED", "QUEUED", "RETRY_WAIT").contains(execution.getFstatus())) {
            throw new IllegalArgumentException("当前状态不允许跳过: " + execution.getFstatus());
        }
        String from = execution.getFstatus();
        transition(execution, "SKIPPED", true);
        cancelPendingOutbox(executionNo, "MANUAL_SKIP");
        saveLog(executionNo, "SKIP", operatorId, reason, from, "SKIPPED");
        return requiredExecution(executionNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerExecution markSuccess(String executionNo, String operatorId, String reason) {
        MatrixSchedulerExecution execution = requiredExecution(executionNo);
        if ("SUCCESS".equals(execution.getFstatus())) {
            return execution;
        }
        String from = execution.getFstatus();
        transition(execution, "SUCCESS", true);
        cancelPendingOutbox(executionNo, "MANUAL_MARK_SUCCESS");
        executionMapper.update(null,
                new LambdaUpdateWrapper<MatrixSchedulerExecution>()
                        .eq(MatrixSchedulerExecution::getFid, execution.getFid())
                        .set(MatrixSchedulerExecution::getFprogress, 100)
                        .set(MatrixSchedulerExecution::getFcurrentStage, "MANUAL_SUCCESS")
                        .set(MatrixSchedulerExecution::getFprogressMessage, "由管理员人工标记成功")
                        .set(MatrixSchedulerExecution::getFlastProgressTime, LocalDateTime.now()));
        saveLog(executionNo, "MARK_SUCCESS", operatorId, reason, from, "SUCCESS");
        return requiredExecution(executionNo);
    }

    public IPage<MatrixSchedulerOperationLog> listLogs(String executionNo, int page, int size) {
        return operationLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MatrixSchedulerOperationLog>()
                        .eq(MatrixSchedulerOperationLog::getFexecutionNo, executionNo)
                        .orderByDesc(MatrixSchedulerOperationLog::getFcreateTime));
    }

    private void transition(MatrixSchedulerExecution execution, String targetStatus, boolean terminal) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<MatrixSchedulerExecution> wrapper = new LambdaUpdateWrapper<MatrixSchedulerExecution>()
                .eq(MatrixSchedulerExecution::getFid, execution.getFid())
                .eq(MatrixSchedulerExecution::getFstatus, execution.getFstatus())
                .set(MatrixSchedulerExecution::getFstatus, targetStatus)
                .set(MatrixSchedulerExecution::getFnextRetryTime, null)
                .set(MatrixSchedulerExecution::getFupdateTime, now);
        if (terminal) {
            wrapper.set(MatrixSchedulerExecution::getFactualEndTime, now);
        }
        ensureUpdated(executionMapper.update(null, wrapper));
    }

    private void cancelPendingOutbox(String executionNo, String reason) {
        outboxMapper.update(null,
                new LambdaUpdateWrapper<MatrixSchedulerOutbox>()
                        .eq(MatrixSchedulerOutbox::getFaggregateId, executionNo)
                        .in(MatrixSchedulerOutbox::getFstatus, "PENDING", "FAILED")
                        .set(MatrixSchedulerOutbox::getFstatus, "CANCELLED")
                        .set(MatrixSchedulerOutbox::getFnextRetryTime, null)
                        .set(MatrixSchedulerOutbox::getFlastError, reason)
                        .set(MatrixSchedulerOutbox::getFupdateTime, LocalDateTime.now()));
    }

    private MatrixSchedulerExecution requiredExecution(String executionNo) {
        MatrixSchedulerExecution execution = executionMapper.selectOne(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .eq(MatrixSchedulerExecution::getFexecutionNo, executionNo)
                        .last("LIMIT 1"));
        if (execution == null) {
            throw new IllegalArgumentException("执行实例不存在");
        }
        return execution;
    }

    private void saveLog(String executionNo,
                         String action,
                         String operatorId,
                         String reason,
                         String fromStatus,
                         String toStatus) {
        MatrixSchedulerOperationLog log = new MatrixSchedulerOperationLog();
        log.setFid(IdWorker.getId());
        log.setFexecutionNo(executionNo);
        log.setFaction(action);
        log.setFoperatorId(operatorId == null || operatorId.isBlank() ? "system" : operatorId);
        log.setFreason(trim(reason, 500));
        log.setFfromStatus(fromStatus);
        log.setFtoStatus(toStatus);
        log.setFcreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private void ensureUpdated(int updated) {
        if (updated != 1) {
            throw new IllegalStateException("执行状态已发生变化，请刷新后重试");
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
