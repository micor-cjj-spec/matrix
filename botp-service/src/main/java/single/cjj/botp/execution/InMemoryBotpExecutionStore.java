package single.cjj.botp.execution;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.ExecutionDetails;
import single.cjj.botp.domain.BotpContracts.ExecutionRequest;
import single.cjj.botp.domain.BotpContracts.ExecutionStatus;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetResult;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "memory")
public class InMemoryBotpExecutionStore implements BotpExecutionStore {

    private final Map<String, ExecutionDetails> byId = new ConcurrentHashMap<>();
    private final Map<String, ExecutionRequest> requestById = new ConcurrentHashMap<>();
    private final Map<String, String> idByRequest = new ConcurrentHashMap<>();

    @Override
    public Optional<ExecutionDetails> findById(String executionId) {
        return Optional.ofNullable(byId.get(executionId));
    }

    @Override
    public Optional<ExecutionRequest> findRequestById(String executionId) {
        return Optional.ofNullable(requestById.get(executionId));
    }

    @Override
    public Optional<ExecutionDetails> findByRequest(String tenantId, String sourceSystem, String requestId) {
        String executionId = idByRequest.get(requestKey(tenantId, sourceSystem, requestId));
        return executionId == null ? Optional.empty() : findById(executionId);
    }

    @Override
    public synchronized ExecutionDetails save(
            ExecutionRequest request,
            RuleDefinition rule,
            String executionId,
            ExecutionStatus status,
            List<TargetResult> targets,
            String errorMessage
    ) {
        ExecutionDetails current = byId.get(executionId);
        LocalDateTime startTime = current == null ? LocalDateTime.now() : current.startTime();
        LocalDateTime finishTime = isTerminal(status) ? LocalDateTime.now() : null;
        ExecutionDetails details = new ExecutionDetails(
                request.tenantId(), request.sourceSystem(), request.requestId(), executionId,
                rule.ruleCode(), rule.version(), request.executionMode(), status,
                request.sourceDocuments(), targets, errorMessage, startTime, finishTime
        );
        byId.put(executionId, details);
        requestById.putIfAbsent(executionId, request);
        idByRequest.putIfAbsent(requestKey(request.tenantId(), request.sourceSystem(), request.requestId()), executionId);
        return details;
    }

    @Override
    public synchronized ExecutionDetails updateStatus(String executionId, ExecutionStatus status, String errorMessage) {
        ExecutionDetails current = findById(executionId)
                .orElseThrow(() -> new BizException("BOTP 执行任务不存在: " + executionId));
        ExecutionDetails updated = new ExecutionDetails(
                current.tenantId(), current.sourceSystem(), current.requestId(), current.executionId(),
                current.ruleCode(), current.ruleVersion(), current.executionMode(), status,
                current.sourceDocuments(), current.targetDocuments(), errorMessage,
                current.startTime(), isTerminal(status) ? LocalDateTime.now() : null
        );
        byId.put(executionId, updated);
        return updated;
    }

    @Override
    public List<ExecutionDetails> list(int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        return byId.values().stream()
                .sorted(Comparator.comparing(ExecutionDetails::startTime).reversed())
                .limit(size).toList();
    }

    private String requestKey(String tenantId, String sourceSystem, String requestId) {
        return tenantId + "|" + sourceSystem + "|" + requestId;
    }

    private boolean isTerminal(ExecutionStatus status) {
        return status == ExecutionStatus.SUCCEEDED
                || status == ExecutionStatus.FAILED
                || status == ExecutionStatus.REVERSED;
    }
}
