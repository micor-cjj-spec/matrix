package single.cjj.botp.execution;

import single.cjj.botp.domain.BotpContracts.ExecutionDetails;
import single.cjj.botp.domain.BotpContracts.ExecutionRequest;
import single.cjj.botp.domain.BotpContracts.ExecutionStatus;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetResult;

import java.util.List;
import java.util.Optional;

public interface BotpExecutionStore {

    Optional<ExecutionDetails> findById(String executionId);

    Optional<ExecutionDetails> findByRequest(String tenantId, String sourceSystem, String requestId);

    ExecutionDetails save(
            ExecutionRequest request,
            RuleDefinition rule,
            String executionId,
            ExecutionStatus status,
            List<TargetResult> targets,
            String errorMessage
    );

    List<ExecutionDetails> list(int limit);
}
