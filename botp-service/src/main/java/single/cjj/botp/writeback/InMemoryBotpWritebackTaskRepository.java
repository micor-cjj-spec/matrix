package single.cjj.botp.writeback;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.TaskStatus;
import single.cjj.botp.domain.BotpContracts.WritebackTask;
import single.cjj.botp.domain.BotpContracts.WritebackTaskType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "memory")
public class InMemoryBotpWritebackTaskRepository implements BotpWritebackTaskRepository {

    private final AtomicLong sequence = new AtomicLong(1);
    private final List<WritebackTask> tasks = new ArrayList<>();

    @Override
    public synchronized WritebackTask create(
            String tenantId,
            String executionId,
            Long relationId,
            DocumentRef source,
            TargetResult target,
            WritebackTaskType taskType,
            BigDecimal activeAllocatedAmount,
            BigDecimal releaseReservedAmount
    ) {
        WritebackTask existing = tasks.stream()
                .filter(item -> executionId.equals(item.executionId()))
                .filter(item -> relationId == null || relationId.equals(item.relationId()))
                .filter(item -> taskType == item.taskType())
                .filter(item -> item.status() == TaskStatus.PENDING || item.status() == TaskStatus.PROCESSING || item.status() == TaskStatus.FAILED)
                .findFirst().orElse(null);
        if (existing != null) {
            return existing;
        }
        WritebackTask task = new WritebackTask(
                sequence.getAndIncrement(), tenantId, executionId, relationId, source, target, taskType,
                TaskStatus.PENDING, activeAllocatedAmount, releaseReservedAmount, 0,
                LocalDateTime.now(), null, LocalDateTime.now(), null
        );
        tasks.add(task);
        return task;
    }

    @Override
    public synchronized Optional<WritebackTask> findById(Long taskId) {
        return tasks.stream().filter(item -> item.taskId().equals(taskId)).findFirst();
    }

    @Override
    public synchronized List<WritebackTask> list(int limit) {
        return tasks.stream()
                .sorted(Comparator.comparing(WritebackTask::createdTime).reversed())
                .limit(Math.max(1, limit)).toList();
    }

    @Override
    public synchronized List<WritebackTask> findDue(LocalDateTime now, int limit) {
        return tasks.stream()
                .filter(item -> item.status() == TaskStatus.PENDING || item.status() == TaskStatus.FAILED)
                .filter(item -> item.nextRetryTime() == null || !item.nextRetryTime().isAfter(now))
                .limit(Math.max(1, limit)).toList();
    }

    @Override
    public synchronized boolean claim(Long taskId) {
        WritebackTask task = require(taskId);
        if (task.status() != TaskStatus.PENDING && task.status() != TaskStatus.FAILED) {
            return false;
        }
        replace(copy(task, TaskStatus.PROCESSING, task.retryCount(), null, task.nextRetryTime(), null));
        return true;
    }

    @Override
    public synchronized WritebackTask markSucceeded(Long taskId) {
        WritebackTask task = require(taskId);
        WritebackTask updated = copy(task, TaskStatus.SUCCEEDED, task.retryCount(), null, null, LocalDateTime.now());
        replace(updated);
        return updated;
    }

    @Override
    public synchronized WritebackTask markFailed(Long taskId, String errorMessage, LocalDateTime nextRetryTime, boolean dead) {
        WritebackTask task = require(taskId);
        WritebackTask updated = copy(
                task,
                dead ? TaskStatus.DEAD : TaskStatus.FAILED,
                task.retryCount() + 1,
                errorMessage,
                dead ? null : nextRetryTime,
                dead ? LocalDateTime.now() : null
        );
        replace(updated);
        return updated;
    }

    @Override
    public synchronized int recoverStale(LocalDateTime cutoff) {
        int count = 0;
        for (WritebackTask task : List.copyOf(tasks)) {
            if (task.status() == TaskStatus.PROCESSING && task.createdTime().isBefore(cutoff)) {
                replace(copy(task, TaskStatus.FAILED, task.retryCount(), "PROCESSING 超时，已自动恢复", LocalDateTime.now(), null));
                count++;
            }
        }
        return count;
    }

    private WritebackTask require(Long taskId) {
        return findById(taskId).orElseThrow(() -> new BizException("BOTP 反写任务不存在: " + taskId));
    }

    private void replace(WritebackTask updated) {
        for (int index = 0; index < tasks.size(); index++) {
            if (tasks.get(index).taskId().equals(updated.taskId())) {
                tasks.set(index, updated);
                return;
            }
        }
    }

    private WritebackTask copy(
            WritebackTask source,
            TaskStatus status,
            int retryCount,
            String errorMessage,
            LocalDateTime nextRetryTime,
            LocalDateTime finishTime
    ) {
        return new WritebackTask(
                source.taskId(), source.tenantId(), source.executionId(), source.relationId(),
                source.sourceDocument(), source.targetDocument(), source.taskType(), status,
                source.activeAllocatedAmount(), source.releaseReservedAmount(), retryCount,
                nextRetryTime, errorMessage, source.createdTime(), finishTime
        );
    }
}
