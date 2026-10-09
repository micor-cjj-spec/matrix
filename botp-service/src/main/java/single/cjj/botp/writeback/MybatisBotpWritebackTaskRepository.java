package single.cjj.botp.writeback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.TaskStatus;
import single.cjj.botp.domain.BotpContracts.WritebackTask;
import single.cjj.botp.domain.BotpContracts.WritebackTaskType;
import single.cjj.botp.persistence.entity.BotpWritebackTaskEntity;
import single.cjj.botp.persistence.mapper.BotpWritebackTaskMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisBotpWritebackTaskRepository implements BotpWritebackTaskRepository {

    private final BotpWritebackTaskMapper mapper;

    public MybatisBotpWritebackTaskRepository(BotpWritebackTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public WritebackTask create(
            String tenantId,
            String executionId,
            Long relationId,
            DocumentRef source,
            TargetResult target,
            WritebackTaskType taskType,
            BigDecimal activeAllocatedAmount,
            BigDecimal releaseReservedAmount
    ) {
        BotpWritebackTaskEntity existing = mapper.selectOne(new LambdaQueryWrapper<BotpWritebackTaskEntity>()
                .eq(BotpWritebackTaskEntity::getFexecutionId, executionId)
                .eq(relationId != null, BotpWritebackTaskEntity::getFrelationId, relationId)
                .eq(BotpWritebackTaskEntity::getFtaskType, taskType.name())
                .in(BotpWritebackTaskEntity::getFstatus, TaskStatus.PENDING.name(), TaskStatus.PROCESSING.name(), TaskStatus.FAILED.name())
                .last("limit 1"));
        if (existing != null) {
            return toDomain(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        BotpWritebackTaskEntity entity = new BotpWritebackTaskEntity();
        entity.setFtenantId(tenantId);
        entity.setFexecutionId(executionId);
        entity.setFrelationId(relationId);
        entity.setFsourceSystemCode(source.systemCode());
        entity.setFsourceDocumentType(source.documentType());
        entity.setFsourceDocumentId(source.documentId());
        entity.setFtargetSystemCode(target == null ? null : target.systemCode());
        entity.setFtargetDocumentType(target == null ? null : target.documentType());
        entity.setFtargetDocumentId(target == null ? null : target.documentId());
        entity.setFtargetDocumentNo(target == null ? null : target.documentNo());
        entity.setFtaskType(taskType.name());
        entity.setFstatus(TaskStatus.PENDING.name());
        entity.setFactiveAllocatedAmount(activeAllocatedAmount);
        entity.setFreleaseReservedAmount(releaseReservedAmount);
        entity.setFcommandJson("{}");
        entity.setFretryCount(0);
        entity.setFnextRetryTime(now);
        entity.setFcreateTime(now);
        entity.setFmodifyTime(now);
        entity.setFdeleteFlag(0);
        entity.setFversion(0);
        mapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<WritebackTask> findById(Long taskId) {
        return Optional.ofNullable(mapper.selectById(taskId)).map(this::toDomain);
    }

    @Override
    public List<WritebackTask> list(int limit) {
        int size = Math.max(1, Math.min(limit, 500));
        return mapper.selectList(new LambdaQueryWrapper<BotpWritebackTaskEntity>()
                        .orderByDesc(BotpWritebackTaskEntity::getFcreateTime)
                        .last("limit " + size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<WritebackTask> findDue(LocalDateTime now, int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        return mapper.selectList(new LambdaQueryWrapper<BotpWritebackTaskEntity>()
                        .in(BotpWritebackTaskEntity::getFstatus, TaskStatus.PENDING.name(), TaskStatus.FAILED.name())
                        .and(wrapper -> wrapper.isNull(BotpWritebackTaskEntity::getFnextRetryTime)
                                .or().le(BotpWritebackTaskEntity::getFnextRetryTime, now))
                        .orderByAsc(BotpWritebackTaskEntity::getFnextRetryTime)
                        .orderByAsc(BotpWritebackTaskEntity::getFcreateTime)
                        .last("limit " + size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean claim(Long taskId) {
        UpdateWrapper<BotpWritebackTaskEntity> update = new UpdateWrapper<>();
        update.eq("fid", taskId)
                .in("fstatus", TaskStatus.PENDING.name(), TaskStatus.FAILED.name())
                .set("fstatus", TaskStatus.PROCESSING.name())
                .set("fmodify_time", LocalDateTime.now())
                .setSql("fversion = fversion + 1");
        return mapper.update(null, update) == 1;
    }

    @Override
    public WritebackTask markSucceeded(Long taskId) {
        BotpWritebackTaskEntity entity = require(taskId);
        entity.setFstatus(TaskStatus.SUCCEEDED.name());
        entity.setFerrorMessage(null);
        entity.setFnextRetryTime(null);
        entity.setFfinishTime(LocalDateTime.now());
        entity.setFmodifyTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toDomain(entity);
    }

    @Override
    public WritebackTask markFailed(Long taskId, String errorMessage, LocalDateTime nextRetryTime, boolean dead) {
        BotpWritebackTaskEntity entity = require(taskId);
        entity.setFretryCount((entity.getFretryCount() == null ? 0 : entity.getFretryCount()) + 1);
        entity.setFstatus(dead ? TaskStatus.DEAD.name() : TaskStatus.FAILED.name());
        entity.setFerrorMessage(errorMessage);
        entity.setFnextRetryTime(dead ? null : nextRetryTime);
        entity.setFfinishTime(dead ? LocalDateTime.now() : null);
        entity.setFmodifyTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toDomain(entity);
    }

    @Override
    public int recoverStale(LocalDateTime cutoff) {
        UpdateWrapper<BotpWritebackTaskEntity> update = new UpdateWrapper<>();
        update.eq("fstatus", TaskStatus.PROCESSING.name())
                .lt("fmodify_time", cutoff)
                .set("fstatus", TaskStatus.FAILED.name())
                .set("fnext_retry_time", LocalDateTime.now())
                .set("ferror_message", "PROCESSING 超时，已自动恢复")
                .set("fmodify_time", LocalDateTime.now())
                .setSql("fversion = fversion + 1");
        return mapper.update(null, update);
    }

    private BotpWritebackTaskEntity require(Long taskId) {
        BotpWritebackTaskEntity entity = mapper.selectById(taskId);
        if (entity == null) {
            throw new BizException("BOTP 反写任务不存在: " + taskId);
        }
        return entity;
    }

    private WritebackTask toDomain(BotpWritebackTaskEntity entity) {
        TargetResult target = entity.getFtargetDocumentId() == null ? null : new TargetResult(
                entity.getFtargetSystemCode(), entity.getFtargetDocumentType(), entity.getFtargetDocumentId(), entity.getFtargetDocumentNo());
        return new WritebackTask(
                entity.getFid(), entity.getFtenantId(), entity.getFexecutionId(), entity.getFrelationId(),
                new DocumentRef(entity.getFsourceSystemCode(), entity.getFsourceDocumentType(), entity.getFsourceDocumentId(), List.of()),
                target, WritebackTaskType.valueOf(entity.getFtaskType()), TaskStatus.valueOf(entity.getFstatus()),
                entity.getFactiveAllocatedAmount(), entity.getFreleaseReservedAmount(),
                entity.getFretryCount() == null ? 0 : entity.getFretryCount(), entity.getFnextRetryTime(),
                entity.getFerrorMessage(), entity.getFcreateTime(), entity.getFfinishTime()
        );
    }
}
