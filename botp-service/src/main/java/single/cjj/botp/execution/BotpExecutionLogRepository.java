package single.cjj.botp.execution;

import single.cjj.botp.domain.BotpContracts.ExecutionLog;
import single.cjj.botp.domain.BotpContracts.TaskStatus;

import java.util.List;

public interface BotpExecutionLogRepository {

    ExecutionLog append(
            String executionId,
            String stage,
            TaskStatus status,
            String message,
            String requestSnapshot,
            String responseSnapshot,
            Throwable exception
    );

    List<ExecutionLog> findByExecutionId(String executionId);
}
