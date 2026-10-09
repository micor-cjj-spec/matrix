package single.cjj.botp.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import single.cjj.botp.domain.BotpContracts.ExecutionLog;
import single.cjj.botp.domain.BotpContracts.TaskStatus;
import single.cjj.botp.persistence.entity.BotpExecutionLogEntity;
import single.cjj.botp.persistence.mapper.BotpExecutionLogMapper;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisBotpExecutionLogRepository implements BotpExecutionLogRepository {

    private final BotpExecutionLogMapper mapper;

    public MybatisBotpExecutionLogRepository(BotpExecutionLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ExecutionLog append(
            String executionId,
            String stage,
            TaskStatus status,
            String message,
            String requestSnapshot,
            String responseSnapshot,
            Throwable exception
    ) {
        LocalDateTime now = LocalDateTime.now();
        BotpExecutionLogEntity entity = new BotpExecutionLogEntity();
        entity.setFexecutionId(executionId);
        entity.setFstage(stage);
        entity.setFstatus(status.name());
        entity.setFmessage(message);
        entity.setFrequestSnapshot(requestSnapshot);
        entity.setFresponseSnapshot(responseSnapshot);
        entity.setFexceptionType(exception == null ? null : exception.getClass().getName());
        entity.setFstartTime(now);
        entity.setFfinishTime(now);
        entity.setFcreateTime(now);
        entity.setFdeleteFlag(0);
        mapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public List<ExecutionLog> findByExecutionId(String executionId) {
        return mapper.selectList(new LambdaQueryWrapper<BotpExecutionLogEntity>()
                        .eq(BotpExecutionLogEntity::getFexecutionId, executionId)
                        .orderByAsc(BotpExecutionLogEntity::getFcreateTime))
                .stream().map(this::toDomain).toList();
    }

    private ExecutionLog toDomain(BotpExecutionLogEntity entity) {
        return new ExecutionLog(
                entity.getFid(), entity.getFexecutionId(), entity.getFstage(),
                TaskStatus.valueOf(entity.getFstatus()), entity.getFmessage(),
                entity.getFrequestSnapshot(), entity.getFresponseSnapshot(), entity.getFexceptionType(),
                entity.getFstartTime(), entity.getFfinishTime()
        );
    }
}
