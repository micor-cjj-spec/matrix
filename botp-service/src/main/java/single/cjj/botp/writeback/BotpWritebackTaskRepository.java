package single.cjj.botp.writeback;

import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.WritebackTask;
import single.cjj.botp.domain.BotpContracts.WritebackTaskType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BotpWritebackTaskRepository {

    WritebackTask create(
            String tenantId,
            String executionId,
            Long relationId,
            DocumentRef source,
            TargetResult target,
            WritebackTaskType taskType,
            BigDecimal activeAllocatedAmount,
            BigDecimal releaseReservedAmount
    );

    Optional<WritebackTask> findById(Long taskId);

    List<WritebackTask> list(int limit);

    List<WritebackTask> findDue(LocalDateTime now, int limit);

    boolean claim(Long taskId);

    WritebackTask markSucceeded(Long taskId);

    WritebackTask markFailed(Long taskId, String errorMessage, LocalDateTime nextRetryTime, boolean dead);

    int recoverStale(LocalDateTime cutoff);
}
