package single.cjj.botp.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.ExecutionDetails;
import single.cjj.botp.domain.BotpContracts.ExecutionMode;
import single.cjj.botp.domain.BotpContracts.ExecutionRequest;
import single.cjj.botp.domain.BotpContracts.ExecutionStatus;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.persistence.entity.BotpExecutionEntity;
import single.cjj.botp.persistence.entity.BotpExecutionTargetEntity;
import single.cjj.botp.persistence.mapper.BotpExecutionMapper;
import single.cjj.botp.persistence.mapper.BotpExecutionTargetMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisBotpExecutionStore implements BotpExecutionStore {

    private final BotpExecutionMapper executionMapper;
    private final BotpExecutionTargetMapper targetMapper;
    private final ObjectMapper objectMapper;

    public MybatisBotpExecutionStore(
            BotpExecutionMapper executionMapper,
            BotpExecutionTargetMapper targetMapper,
            ObjectMapper objectMapper
    ) {
        this.executionMapper = executionMapper;
        this.targetMapper = targetMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ExecutionDetails> findById(String executionId) {
        BotpExecutionEntity entity = executionMapper.selectOne(new LambdaQueryWrapper<BotpExecutionEntity>()
                .eq(BotpExecutionEntity::getFexecutionId, executionId)
                .last("limit 1"));
        return Optional.ofNullable(entity).map(this::toDetails);
    }

    @Override
    public Optional<ExecutionDetails> findByRequest(String tenantId, String sourceSystem, String requestId) {
        BotpExecutionEntity entity = executionMapper.selectOne(new LambdaQueryWrapper<BotpExecutionEntity>()
                .eq(BotpExecutionEntity::getFtenantId, tenantId)
                .eq(BotpExecutionEntity::getFsourceSystem, sourceSystem)
                .eq(BotpExecutionEntity::getFrequestId, requestId)
                .last("limit 1"));
        return Optional.ofNullable(entity).map(this::toDetails);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExecutionDetails save(
            ExecutionRequest request,
            RuleDefinition rule,
            String executionId,
            ExecutionStatus status,
            List<TargetResult> targets,
            String errorMessage
    ) {
        LocalDateTime now = LocalDateTime.now();
        BotpExecutionEntity entity = executionMapper.selectOne(new LambdaQueryWrapper<BotpExecutionEntity>()
                .eq(BotpExecutionEntity::getFexecutionId, executionId)
                .last("limit 1"));
        boolean insert = entity == null;
        if (insert) {
            entity = new BotpExecutionEntity();
            entity.setFtenantId(request.tenantId());
            entity.setFexecutionId(executionId);
            entity.setFsourceSystem(request.sourceSystem());
            entity.setFrequestId(request.requestId());
            entity.setFruleCode(rule.ruleCode());
            entity.setFruleVersion(rule.version());
            entity.setFexecutionMode(request.executionMode().name());
            entity.setFrequestJson(serializeRequest(request));
            entity.setFretryCount(0);
            entity.setFstartTime(now);
            entity.setFcreateTime(now);
            entity.setFdeleteFlag(0);
            entity.setFversion(0);
        }
        entity.setFstatus(status.name());
        entity.setFerrorMessage(errorMessage);
        entity.setFfinishTime(isTerminal(status) ? now : null);
        entity.setFmodifyTime(now);
        if (insert) {
            executionMapper.insert(entity);
        } else {
            executionMapper.updateById(entity);
        }

        for (int index = 0; index < targets.size(); index++) {
            upsertTarget(request.tenantId(), executionId, index, targets.get(index), now);
        }
        return toDetails(entity);
    }

    @Override
    public List<ExecutionDetails> list(int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        return executionMapper.selectList(new LambdaQueryWrapper<BotpExecutionEntity>()
                        .orderByDesc(BotpExecutionEntity::getFstartTime)
                        .last("limit " + size))
                .stream()
                .map(this::toDetails)
                .toList();
    }

    private void upsertTarget(
            String tenantId,
            String executionId,
            int index,
            TargetResult target,
            LocalDateTime now
    ) {
        BotpExecutionTargetEntity entity = targetMapper.selectOne(
                new LambdaQueryWrapper<BotpExecutionTargetEntity>()
                        .eq(BotpExecutionTargetEntity::getFexecutionId, executionId)
                        .eq(BotpExecutionTargetEntity::getFtargetIndex, index)
                        .last("limit 1")
        );
        boolean insert = entity == null;
        if (insert) {
            entity = new BotpExecutionTargetEntity();
            entity.setFtenantId(tenantId);
            entity.setFexecutionId(executionId);
            entity.setFtargetIndex(index);
            entity.setFtargetIdempotencyKey(targetKey(tenantId, executionId, index));
            entity.setFcreateTime(now);
            entity.setFdeleteFlag(0);
            entity.setFversion(0);
        }
        entity.setFtargetSystemCode(target.systemCode());
        entity.setFtargetDocumentType(target.documentType());
        entity.setFtargetDocumentId(target.documentId());
        entity.setFtargetDocumentNo(target.documentNo());
        entity.setFstatus("CREATED");
        entity.setFmodifyTime(now);
        if (insert) {
            targetMapper.insert(entity);
        } else {
            targetMapper.updateById(entity);
        }
    }

    private ExecutionDetails toDetails(BotpExecutionEntity entity) {
        ExecutionRequest request = deserializeRequest(entity.getFrequestJson());
        List<TargetResult> targets = targetMapper.selectList(
                        new LambdaQueryWrapper<BotpExecutionTargetEntity>()
                                .eq(BotpExecutionTargetEntity::getFexecutionId, entity.getFexecutionId())
                                .orderByAsc(BotpExecutionTargetEntity::getFtargetIndex))
                .stream()
                .map(item -> new TargetResult(
                        item.getFtargetSystemCode(),
                        item.getFtargetDocumentType(),
                        item.getFtargetDocumentId(),
                        item.getFtargetDocumentNo()
                ))
                .toList();
        return new ExecutionDetails(
                entity.getFtenantId(),
                entity.getFsourceSystem(),
                entity.getFrequestId(),
                entity.getFexecutionId(),
                entity.getFruleCode(),
                entity.getFruleVersion(),
                ExecutionMode.valueOf(entity.getFexecutionMode()),
                ExecutionStatus.valueOf(entity.getFstatus()),
                request.sourceDocuments(),
                targets,
                entity.getFerrorMessage(),
                entity.getFstartTime(),
                entity.getFfinishTime()
        );
    }

    private String serializeRequest(ExecutionRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new BizException("BOTP 执行请求序列化失败: " + request.requestId());
        }
    }

    private ExecutionRequest deserializeRequest(String json) {
        try {
            return objectMapper.readValue(json, ExecutionRequest.class);
        } catch (JsonProcessingException exception) {
            throw new BizException("BOTP 执行请求快照解析失败");
        }
    }

    private String targetKey(String tenantId, String executionId, int index) {
        return "botp:" + tenantId + ":" + executionId + ":" + index;
    }

    private boolean isTerminal(ExecutionStatus status) {
        return status == ExecutionStatus.SUCCEEDED
                || status == ExecutionStatus.FAILED
                || status == ExecutionStatus.REVERSED;
    }
}
