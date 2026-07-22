package single.cjj.botp.execution;

import single.cjj.botp.domain.BotpContracts.ExecutionRequest;
import single.cjj.botp.domain.BotpContracts.ExecutionResult;
import single.cjj.botp.domain.BotpContracts.PreviewResult;

public interface BotpExecutionService {

    PreviewResult preview(ExecutionRequest request);

    ExecutionResult execute(ExecutionRequest request);

    ExecutionResult getById(String executionId);
}
