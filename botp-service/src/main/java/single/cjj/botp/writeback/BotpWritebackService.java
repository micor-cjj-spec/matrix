package single.cjj.botp.writeback;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.adapter.BotpAdapterRegistry;
import single.cjj.botp.adapter.BotpDocumentAdapter;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.TaskStatus;
import single.cjj.botp.domain.BotpContracts.WritebackCommand;
import single.cjj.botp.domain.BotpContracts.WritebackTask;
import single.cjj.botp.domain.BotpContracts.WritebackTaskType;
import single.cjj.botp.execution.BotpExecutionLogRepository;
import single.cjj.botp.relation.BotpRelationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BotpWritebackService {

    private static final int MAX_RETRY_COUNT = 5;

    private final BotpWritebackTaskRepository taskRepository;
    private final BotpAdapterRegistry adapterRegistry;
    private final BotpRelationRepository relationRepository;
    private final BotpExecutionLogRepository logRepository;

    public BotpWritebackService(
            BotpWritebackTaskRepository taskRepository,
            BotpAdapterRegistry adapterRegistry,
            BotpRelationRepository relationRepository,
            BotpExecutionLogRepository logRepository
    ) {
        this.taskRepository = taskRepository;
        this.adapterRegistry = adapterRegistry;
        this.relationRepository = relationRepository;
        this.logRepository = logRepository;
    }

    public WritebackTask enqueueForward(
            String tenantId,
            String executionId,
            DocumentRelation relation,
            BigDecimal activeAllocatedAmount,
            BigDecimal releaseReservedAmount
    ) {
        return taskRepository.create(
                tenantId, executionId, relation.relationId(), relation.sourceDocument(), relation.targetDocument(),
                WritebackTaskType.FORWARD_WRITEBACK, activeAllocatedAmount, releaseReservedAmount
        );
    }

    public WritebackTask enqueueReverse(DocumentRelation relation, BigDecimal activeAllocatedAmount) {
        return taskRepository.create(
                relation.tenantId(), relation.executionId(), relation.relationId(), relation.sourceDocument(), relation.targetDocument(),
                WritebackTaskType.REVERSE_WRITEBACK, activeAllocatedAmount, BigDecimal.ZERO
        );
    }

    public WritebackTask enqueueRecompute(
            String tenantId,
            String executionId,
            DocumentRelation relation,
            BigDecimal activeAllocatedAmount
    ) {
        return taskRepository.create(
                tenantId, executionId, relation.relationId(), relation.sourceDocument(), relation.targetDocument(),
                WritebackTaskType.RECOMPUTE_WRITEBACK, activeAllocatedAmount, BigDecimal.ZERO
        );
    }

    public List<WritebackTask> list(int limit) {
        return taskRepository.list(limit);
    }

    public List<WritebackTask> findByExecution(String executionId) {
        return taskRepository.list(500).stream()
                .filter(item -> executionId.equals(item.executionId()))
                .toList();
    }

    public List<WritebackTask> retryByExecution(String executionId) {
        List<WritebackTask> tasks = findByExecution(executionId);
        for (WritebackTask task : tasks) {
            if (task.status() != TaskStatus.SUCCEEDED) {
                retryNow(task.taskId());
            }
        }
        return findByExecution(executionId);
    }

    public WritebackTask retryNow(Long taskId) {
        WritebackTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException("BOTP 反写任务不存在: " + taskId));
        if (task.status() == TaskStatus.SUCCEEDED) {
            return task;
        }
        if (task.status() == TaskStatus.DEAD) {
            taskRepository.markFailed(taskId, "人工重新打开 DEAD 任务", LocalDateTime.now(), false);
        }
        processTask(taskId);
        return taskRepository.findById(taskId).orElseThrow();
    }

    @Scheduled(fixedDelayString = "${botp.writeback.scan-interval-ms:10000}")
    public void processDueTasks() {
        taskRepository.recoverStale(LocalDateTime.now().minusMinutes(5));
        for (WritebackTask task : taskRepository.findDue(LocalDateTime.now(), 50)) {
            processTask(task.taskId());
        }
    }

    public void processTask(Long taskId) {
        if (!taskRepository.claim(taskId)) {
            return;
        }
        WritebackTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException("BOTP 反写任务不存在: " + taskId));
        logRepository.append(
                task.executionId(), task.taskType().name(), TaskStatus.PROCESSING,
                "开始执行反写补偿任务 " + task.taskId(), null, null, null
        );
        try {
            if (task.taskType() == WritebackTaskType.REVERSE_WRITEBACK && task.relationId() != null) {
                relationRepository.markReversing(task.relationId());
            }
            BotpDocumentAdapter sourceAdapter = adapterRegistry.require(
                    task.sourceDocument().systemCode(), task.sourceDocument().documentType());
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("activeAllocatedAmount", nz(task.activeAllocatedAmount()));
            context.put("releaseReservedAmount", nz(task.releaseReservedAmount()));
            context.put("writebackTaskId", task.taskId());
            context.put("writebackTaskType", task.taskType().name());
            sourceAdapter.applyWriteback(new WritebackCommand(
                    task.executionId(), task.sourceDocument(), task.targetDocument(), List.of(), context
            ));
            if (task.taskType() == WritebackTaskType.REVERSE_WRITEBACK && task.relationId() != null) {
                relationRepository.markReversed(task.relationId());
            }
            taskRepository.markSucceeded(task.taskId());
            logRepository.append(
                    task.executionId(), task.taskType().name(), TaskStatus.SUCCEEDED,
                    "反写补偿任务执行成功 " + task.taskId(), null, null, null
            );
        } catch (RuntimeException exception) {
            int nextAttempt = task.retryCount() + 1;
            boolean dead = nextAttempt >= MAX_RETRY_COUNT;
            LocalDateTime nextRetryTime = dead ? null : LocalDateTime.now().plusSeconds(retryDelaySeconds(nextAttempt));
            taskRepository.markFailed(task.taskId(), safeMessage(exception), nextRetryTime, dead);
            logRepository.append(
                    task.executionId(), task.taskType().name(), dead ? TaskStatus.DEAD : TaskStatus.FAILED,
                    dead ? "反写任务超过最大重试次数" : "反写任务失败，等待自动重试",
                    null, null, exception
            );
        }
    }

    private long retryDelaySeconds(int attempt) {
        return switch (attempt) {
            case 1 -> 10L;
            case 2 -> 30L;
            case 3 -> 120L;
            case 4 -> 600L;
            default -> 1800L;
        };
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null || throwable.getMessage().isBlank()
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }
}
