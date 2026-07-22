package single.cjj.botp.execution;

import org.springframework.stereotype.Service;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.ExecutionDetails;
import single.cjj.botp.domain.BotpContracts.ExecutionRequest;
import single.cjj.botp.domain.BotpContracts.ExecutionStatus;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.TaskStatus;
import single.cjj.botp.domain.BotpContracts.WritebackTask;
import single.cjj.botp.relation.BotpRelationRepository;
import single.cjj.botp.rule.BotpRuleRepository;
import single.cjj.botp.writeback.BotpWritebackService;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BotpRecoveryService {

    private final BotpExecutionStore executionStore;
    private final BotpRuleRepository ruleRepository;
    private final BotpRelationRepository relationRepository;
    private final BotpWritebackService writebackService;
    private final BotpExecutionLogRepository logRepository;

    public BotpRecoveryService(
            BotpExecutionStore executionStore,
            BotpRuleRepository ruleRepository,
            BotpRelationRepository relationRepository,
            BotpWritebackService writebackService,
            BotpExecutionLogRepository logRepository
    ) {
        this.executionStore = executionStore;
        this.ruleRepository = ruleRepository;
        this.relationRepository = relationRepository;
        this.writebackService = writebackService;
        this.logRepository = logRepository;
    }

    public ExecutionDetails retryWriteback(String executionId) {
        ExecutionDetails details = requireExecution(executionId);
        List<WritebackTask> tasks = writebackService.retryByExecution(executionId);
        if (tasks.isEmpty()) {
            throw new BizException("执行任务没有可重试的反写任务: " + executionId);
        }
        boolean success = tasks.stream().allMatch(item -> item.status() == TaskStatus.SUCCEEDED);
        return executionStore.updateStatus(
                executionId,
                success ? ExecutionStatus.SUCCEEDED : ExecutionStatus.WRITEBACK_PENDING,
                success ? null : "仍有反写任务未成功"
        );
    }

    public ExecutionDetails resume(String executionId) {
        ExecutionDetails details = requireExecution(executionId);
        if (details.status() == ExecutionStatus.SUCCEEDED || details.status() == ExecutionStatus.REVERSED) {
            return details;
        }
        List<WritebackTask> existingTasks = writebackService.findByExecution(executionId);
        if (!existingTasks.isEmpty()) {
            return retryWriteback(executionId);
        }
        if (details.targetDocuments().isEmpty()) {
            throw new BizException("当前恢复接口仅支持目标单已创建后的阶段，未发现目标单: " + executionId);
        }

        ExecutionRequest request = executionStore.findRequestById(executionId)
                .orElseThrow(() -> new BizException("执行请求快照不存在: " + executionId));
        RuleDefinition rule = ruleRepository.findVersions(details.ruleCode()).stream()
                .filter(item -> item.version() == details.ruleVersion())
                .findFirst()
                .orElseGet(() -> ruleRepository.findPublishedByCode(details.ruleCode())
                        .orElseThrow(() -> new BizException("执行规则不存在: " + details.ruleCode())));

        int count = Math.min(request.sourceDocuments().size(), details.targetDocuments().size());
        if (count == 0) {
            throw new BizException("执行任务缺少可恢复的源单与目标单配对");
        }
        BigDecimal allocatedAmount = resolveAllocatedAmount(request);
        for (int index = 0; index < count; index++) {
            DocumentRef source = request.sourceDocuments().get(index);
            TargetResult target = details.targetDocuments().get(index);
            DocumentRelation relation = relationRepository.saveActive(
                    request.tenantId(), executionId, rule, source, target, allocatedAmount);
            BigDecimal activeAmount = relationRepository.sumActiveAmount(request.tenantId(), source);
            WritebackTask task = writebackService.enqueueRecompute(
                    request.tenantId(), executionId, relation, activeAmount);
            writebackService.processTask(task.taskId());
        }
        boolean success = writebackService.findByExecution(executionId).stream()
                .allMatch(item -> item.status() == TaskStatus.SUCCEEDED);
        logRepository.append(
                executionId, "RESUME", success ? TaskStatus.SUCCEEDED : TaskStatus.FAILED,
                success ? "执行恢复成功" : "执行恢复后仍有反写任务失败", null, null, null
        );
        return executionStore.updateStatus(
                executionId,
                success ? ExecutionStatus.SUCCEEDED : ExecutionStatus.WRITEBACK_PENDING,
                success ? null : "恢复后仍有反写任务未成功"
        );
    }

    private ExecutionDetails requireExecution(String executionId) {
        return executionStore.findById(executionId)
                .orElseThrow(() -> new BizException("BOTP 执行任务不存在: " + executionId));
    }

    private BigDecimal resolveAllocatedAmount(ExecutionRequest request) {
        Object value = request.parameters().get("pushAmount");
        if (value == null) {
            value = request.parameters().get("allocatedAmount");
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value != null) {
            try {
                return new BigDecimal(String.valueOf(value));
            } catch (NumberFormatException exception) {
                throw new BizException("执行请求中的分配金额格式错误: " + value);
            }
        }
        return BigDecimal.ZERO;
    }
}
