package single.cjj.botp.execution;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import single.cjj.botp.domain.BotpContracts.ExecutionLog;
import single.cjj.botp.domain.BotpContracts.TaskStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "memory")
public class InMemoryBotpExecutionLogRepository implements BotpExecutionLogRepository {

    private final AtomicLong sequence = new AtomicLong(1);
    private final List<ExecutionLog> logs = new ArrayList<>();

    @Override
    public synchronized ExecutionLog append(
            String executionId,
            String stage,
            TaskStatus status,
            String message,
            String requestSnapshot,
            String responseSnapshot,
            Throwable exception
    ) {
        LocalDateTime now = LocalDateTime.now();
        ExecutionLog log = new ExecutionLog(
                sequence.getAndIncrement(), executionId, stage, status, message,
                requestSnapshot, responseSnapshot,
                exception == null ? null : exception.getClass().getName(), now, now
        );
        logs.add(log);
        return log;
    }

    @Override
    public synchronized List<ExecutionLog> findByExecutionId(String executionId) {
        return logs.stream().filter(item -> executionId.equals(item.executionId())).toList();
    }
}
