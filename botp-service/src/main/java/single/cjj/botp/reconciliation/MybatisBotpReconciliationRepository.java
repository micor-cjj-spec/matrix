package single.cjj.botp.reconciliation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.ReconciliationIssue;
import single.cjj.botp.domain.BotpContracts.ReconciliationIssueType;
import single.cjj.botp.domain.BotpContracts.ReconciliationStatus;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.persistence.entity.BotpReconciliationIssueEntity;
import single.cjj.botp.persistence.mapper.BotpReconciliationIssueMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisBotpReconciliationRepository implements BotpReconciliationRepository {

    private final BotpReconciliationIssueMapper mapper;

    public MybatisBotpReconciliationRepository(BotpReconciliationIssueMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ReconciliationIssue saveOpen(
            String tenantId,
            ReconciliationIssueType issueType,
            DocumentRelation relation,
            BigDecimal expectedAmount,
            BigDecimal actualAmount,
            String description
    ) {
        BotpReconciliationIssueEntity existing = mapper.selectOne(
                new LambdaQueryWrapper<BotpReconciliationIssueEntity>()
                        .eq(BotpReconciliationIssueEntity::getFtenantId, tenantId)
                        .eq(BotpReconciliationIssueEntity::getFissueType, issueType.name())
                        .eq(BotpReconciliationIssueEntity::getFrelationId, relation.relationId())
                        .eq(BotpReconciliationIssueEntity::getFstatus, ReconciliationStatus.OPEN.name())
                        .last("limit 1")
        );
        if (existing != null) {
            existing.setFexpectedAmount(expectedAmount);
            existing.setFactualAmount(actualAmount);
            existing.setFdescription(description);
            existing.setFmodifyTime(LocalDateTime.now());
            mapper.updateById(existing);
            return toDomain(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        BotpReconciliationIssueEntity entity = new BotpReconciliationIssueEntity();
        entity.setFtenantId(tenantId);
        entity.setFissueType(issueType.name());
        entity.setFstatus(ReconciliationStatus.OPEN.name());
        entity.setFexecutionId(relation.executionId());
        entity.setFrelationId(relation.relationId());
        entity.setFsourceSystemCode(relation.sourceDocument().systemCode());
        entity.setFsourceDocumentType(relation.sourceDocument().documentType());
        entity.setFsourceDocumentId(relation.sourceDocument().documentId());
        entity.setFtargetSystemCode(relation.targetDocument().systemCode());
        entity.setFtargetDocumentType(relation.targetDocument().documentType());
        entity.setFtargetDocumentId(relation.targetDocument().documentId());
        entity.setFtargetDocumentNo(relation.targetDocument().documentNo());
        entity.setFexpectedAmount(expectedAmount);
        entity.setFactualAmount(actualAmount);
        entity.setFdescription(description);
        entity.setFdetectedTime(now);
        entity.setFcreateTime(now);
        entity.setFmodifyTime(now);
        entity.setFdeleteFlag(0);
        entity.setFversion(0);
        mapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<ReconciliationIssue> findById(Long issueId) {
        return Optional.ofNullable(mapper.selectById(issueId)).map(this::toDomain);
    }

    @Override
    public List<ReconciliationIssue> list(int limit) {
        int size = Math.max(1, Math.min(limit, 500));
        return mapper.selectList(new LambdaQueryWrapper<BotpReconciliationIssueEntity>()
                        .orderByDesc(BotpReconciliationIssueEntity::getFdetectedTime)
                        .last("limit " + size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public ReconciliationIssue markFixed(Long issueId, String resolution) {
        return resolve(issueId, ReconciliationStatus.FIXED, resolution);
    }

    @Override
    public ReconciliationIssue markIgnored(Long issueId, String resolution) {
        return resolve(issueId, ReconciliationStatus.IGNORED, resolution);
    }

    private ReconciliationIssue resolve(Long issueId, ReconciliationStatus status, String resolution) {
        BotpReconciliationIssueEntity entity = mapper.selectById(issueId);
        if (entity == null) {
            throw new BizException("BOTP 对账异常不存在: " + issueId);
        }
        entity.setFstatus(status.name());
        entity.setFresolution(resolution);
        entity.setFresolvedTime(LocalDateTime.now());
        entity.setFmodifyTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toDomain(entity);
    }

    private ReconciliationIssue toDomain(BotpReconciliationIssueEntity entity) {
        DocumentRef source = new DocumentRef(
                entity.getFsourceSystemCode(), entity.getFsourceDocumentType(), entity.getFsourceDocumentId(), List.of());
        TargetResult target = entity.getFtargetDocumentId() == null ? null : new TargetResult(
                entity.getFtargetSystemCode(), entity.getFtargetDocumentType(), entity.getFtargetDocumentId(), entity.getFtargetDocumentNo());
        return new ReconciliationIssue(
                entity.getFid(), entity.getFtenantId(), ReconciliationIssueType.valueOf(entity.getFissueType()),
                ReconciliationStatus.valueOf(entity.getFstatus()), entity.getFexecutionId(), entity.getFrelationId(),
                source, target, entity.getFexpectedAmount(), entity.getFactualAmount(), entity.getFdescription(),
                entity.getFresolution(), entity.getFdetectedTime(), entity.getFresolvedTime()
        );
    }
}
