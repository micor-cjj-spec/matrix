package single.cjj.botp.relation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.RelationStatus;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.persistence.entity.BotpDocumentRelationEntity;
import single.cjj.botp.persistence.mapper.BotpDocumentRelationMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisBotpRelationRepository implements BotpRelationRepository {

    private final BotpDocumentRelationMapper mapper;

    public MybatisBotpRelationRepository(BotpDocumentRelationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentRelation saveActive(
            String tenantId,
            String executionId,
            RuleDefinition rule,
            DocumentRef source,
            TargetResult target,
            BigDecimal allocatedAmount
    ) {
        BotpDocumentRelationEntity existing = mapper.selectOne(
                new LambdaQueryWrapper<BotpDocumentRelationEntity>()
                        .eq(BotpDocumentRelationEntity::getFexecutionId, executionId)
                        .eq(BotpDocumentRelationEntity::getFsourceDocumentId, source.documentId())
                        .eq(BotpDocumentRelationEntity::getFtargetDocumentId, target.documentId())
                        .last("limit 1")
        );
        if (existing != null) {
            return toRelation(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        BotpDocumentRelationEntity entity = new BotpDocumentRelationEntity();
        entity.setFtenantId(tenantId);
        entity.setFexecutionId(executionId);
        entity.setFruleCode(rule.ruleCode());
        entity.setFruleVersion(rule.version());
        entity.setFsourceSystemCode(source.systemCode());
        entity.setFsourceDocumentType(source.documentType());
        entity.setFsourceDocumentId(source.documentId());
        entity.setFtargetSystemCode(target.systemCode());
        entity.setFtargetDocumentType(target.documentType());
        entity.setFtargetDocumentId(target.documentId());
        entity.setFtargetDocumentNo(target.documentNo());
        entity.setFallocatedAmount(allocatedAmount);
        entity.setFrelationStatus(RelationStatus.ACTIVE.name());
        entity.setFtargetStatus("ACTIVE");
        entity.setFcreateTime(now);
        entity.setFmodifyTime(now);
        entity.setFdeleteFlag(0);
        entity.setFversion(0);
        mapper.insert(entity);
        return toRelation(entity);
    }

    @Override
    public BigDecimal sumActiveAmount(String tenantId, DocumentRef source) {
        return mapper.selectList(new LambdaQueryWrapper<BotpDocumentRelationEntity>()
                        .eq(BotpDocumentRelationEntity::getFtenantId, tenantId)
                        .eq(BotpDocumentRelationEntity::getFsourceSystemCode, source.systemCode())
                        .eq(BotpDocumentRelationEntity::getFsourceDocumentType, source.documentType())
                        .eq(BotpDocumentRelationEntity::getFsourceDocumentId, source.documentId())
                        .eq(BotpDocumentRelationEntity::getFrelationStatus, RelationStatus.ACTIVE.name()))
                .stream()
                .map(BotpDocumentRelationEntity::getFallocatedAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Optional<DocumentRelation> findById(Long relationId) {
        return Optional.ofNullable(mapper.selectById(relationId)).map(this::toRelation);
    }

    @Override
    public List<DocumentRelation> findByTarget(String tenantId, String targetDocumentId) {
        return mapper.selectList(new LambdaQueryWrapper<BotpDocumentRelationEntity>()
                        .eq(BotpDocumentRelationEntity::getFtenantId, tenantId)
                        .eq(BotpDocumentRelationEntity::getFtargetDocumentId, targetDocumentId)
                        .orderByDesc(BotpDocumentRelationEntity::getFcreateTime))
                .stream().map(this::toRelation).toList();
    }

    @Override
    public List<DocumentRelation> find(String tenantId, String sourceDocumentId, String targetDocumentId, int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        LambdaQueryWrapper<BotpDocumentRelationEntity> query = new LambdaQueryWrapper<BotpDocumentRelationEntity>()
                .eq(StringUtils.hasText(tenantId), BotpDocumentRelationEntity::getFtenantId, tenantId)
                .eq(StringUtils.hasText(sourceDocumentId), BotpDocumentRelationEntity::getFsourceDocumentId, sourceDocumentId)
                .eq(StringUtils.hasText(targetDocumentId), BotpDocumentRelationEntity::getFtargetDocumentId, targetDocumentId)
                .orderByDesc(BotpDocumentRelationEntity::getFcreateTime)
                .last("limit " + size);
        return mapper.selectList(query).stream().map(this::toRelation).toList();
    }

    @Override
    public List<DocumentRelation> findActive(int limit) {
        int size = Math.max(1, Math.min(limit, 1000));
        return mapper.selectList(new LambdaQueryWrapper<BotpDocumentRelationEntity>()
                        .eq(BotpDocumentRelationEntity::getFrelationStatus, RelationStatus.ACTIVE.name())
                        .orderByAsc(BotpDocumentRelationEntity::getFmodifyTime)
                        .last("limit " + size))
                .stream().map(this::toRelation).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DocumentRelation> invalidateByTarget(
            String tenantId,
            String targetDocumentId,
            String eventId,
            String targetStatus,
            String reason
    ) {
        List<BotpDocumentRelationEntity> entities = mapper.selectList(
                new LambdaQueryWrapper<BotpDocumentRelationEntity>()
                        .eq(BotpDocumentRelationEntity::getFtenantId, tenantId)
                        .eq(BotpDocumentRelationEntity::getFtargetDocumentId, targetDocumentId)
        );
        List<DocumentRelation> changed = new ArrayList<>();
        for (BotpDocumentRelationEntity entity : entities) {
            if (eventId.equals(entity.getFlastEventId())) {
                continue;
            }
            RelationStatus status = RelationStatus.valueOf(entity.getFrelationStatus());
            if (status != RelationStatus.ACTIVE && status != RelationStatus.PENDING) {
                continue;
            }
            entity.setFrelationStatus(RelationStatus.INVALID.name());
            entity.setFtargetStatus(targetStatus);
            entity.setFlastEventId(eventId);
            entity.setFinvalidReason(reason);
            entity.setFinvalidTime(LocalDateTime.now());
            entity.setFmodifyTime(LocalDateTime.now());
            mapper.updateById(entity);
            changed.add(toRelation(entity));
        }
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentRelation invalidateById(Long relationId, String eventId, String reason) {
        BotpDocumentRelationEntity entity = requireEntity(relationId);
        if (eventId.equals(entity.getFlastEventId())) {
            return toRelation(entity);
        }
        RelationStatus status = RelationStatus.valueOf(entity.getFrelationStatus());
        if (status == RelationStatus.ACTIVE || status == RelationStatus.PENDING) {
            entity.setFrelationStatus(RelationStatus.INVALID.name());
            entity.setFtargetStatus("MANUAL_INVALID");
            entity.setFlastEventId(eventId);
            entity.setFinvalidReason(reason);
            entity.setFinvalidTime(LocalDateTime.now());
            entity.setFmodifyTime(LocalDateTime.now());
            mapper.updateById(entity);
        }
        return toRelation(entity);
    }

    @Override
    public DocumentRelation markReversing(Long relationId) {
        BotpDocumentRelationEntity entity = requireEntity(relationId);
        if (RelationStatus.INVALID.name().equals(entity.getFrelationStatus())) {
            entity.setFrelationStatus(RelationStatus.REVERSING.name());
            entity.setFmodifyTime(LocalDateTime.now());
            mapper.updateById(entity);
        }
        return toRelation(entity);
    }

    @Override
    public DocumentRelation markReversed(Long relationId) {
        BotpDocumentRelationEntity entity = requireEntity(relationId);
        RelationStatus status = RelationStatus.valueOf(entity.getFrelationStatus());
        if (status == RelationStatus.INVALID || status == RelationStatus.REVERSING) {
            entity.setFrelationStatus(RelationStatus.REVERSED.name());
            entity.setFreversedTime(LocalDateTime.now());
            entity.setFmodifyTime(LocalDateTime.now());
            mapper.updateById(entity);
        }
        return toRelation(entity);
    }

    private BotpDocumentRelationEntity requireEntity(Long relationId) {
        BotpDocumentRelationEntity entity = mapper.selectById(relationId);
        if (entity == null) {
            throw new BizException("BOTP 单据关系不存在: " + relationId);
        }
        return entity;
    }

    private DocumentRelation toRelation(BotpDocumentRelationEntity entity) {
        return new DocumentRelation(
                entity.getFid(),
                entity.getFtenantId(),
                entity.getFexecutionId(),
                entity.getFruleCode(),
                entity.getFruleVersion(),
                new DocumentRef(entity.getFsourceSystemCode(), entity.getFsourceDocumentType(), entity.getFsourceDocumentId(), List.of()),
                new TargetResult(entity.getFtargetSystemCode(), entity.getFtargetDocumentType(), entity.getFtargetDocumentId(), entity.getFtargetDocumentNo()),
                entity.getFallocatedAmount(),
                RelationStatus.valueOf(entity.getFrelationStatus()),
                entity.getFtargetStatus(),
                entity.getFlastEventId(),
                entity.getFinvalidReason(),
                entity.getFcreateTime(),
                entity.getFinvalidTime(),
                entity.getFreversedTime()
        );
    }
}
