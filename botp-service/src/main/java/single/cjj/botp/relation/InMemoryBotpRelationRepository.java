package single.cjj.botp.relation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.RelationStatus;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "memory")
public class InMemoryBotpRelationRepository implements BotpRelationRepository {

    private final AtomicLong sequence = new AtomicLong(1);
    private final List<DocumentRelation> relations = new ArrayList<>();

    @Override
    public synchronized DocumentRelation saveActive(
            String tenantId,
            String executionId,
            RuleDefinition rule,
            DocumentRef source,
            TargetResult target,
            BigDecimal allocatedAmount
    ) {
        DocumentRelation existing = relations.stream()
                .filter(item -> item.executionId().equals(executionId))
                .filter(item -> item.sourceDocument().documentId().equals(source.documentId()))
                .filter(item -> item.targetDocument().documentId().equals(target.documentId()))
                .findFirst().orElse(null);
        if (existing != null) {
            return existing;
        }
        DocumentRelation relation = new DocumentRelation(
                sequence.getAndIncrement(), tenantId, executionId, rule.ruleCode(), rule.version(), source, target,
                allocatedAmount, RelationStatus.ACTIVE, "ACTIVE", null, null,
                LocalDateTime.now(), null, null
        );
        relations.add(relation);
        return relation;
    }

    @Override
    public synchronized BigDecimal sumActiveAmount(String tenantId, DocumentRef source) {
        return relations.stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .filter(item -> item.status() == RelationStatus.ACTIVE)
                .filter(item -> source.systemCode().equals(item.sourceDocument().systemCode()))
                .filter(item -> source.documentType().equals(item.sourceDocument().documentType()))
                .filter(item -> source.documentId().equals(item.sourceDocument().documentId()))
                .map(DocumentRelation::allocatedAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public synchronized Optional<DocumentRelation> findById(Long relationId) {
        return relations.stream().filter(item -> item.relationId().equals(relationId)).findFirst();
    }

    @Override
    public synchronized List<DocumentRelation> findByTarget(String tenantId, String targetDocumentId) {
        return relations.stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .filter(item -> targetDocumentId.equals(item.targetDocument().documentId()))
                .toList();
    }

    @Override
    public synchronized List<DocumentRelation> find(String tenantId, String sourceDocumentId, String targetDocumentId, int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        return relations.stream()
                .filter(item -> tenantId == null || tenantId.equals(item.tenantId()))
                .filter(item -> sourceDocumentId == null || sourceDocumentId.isBlank() || sourceDocumentId.equals(item.sourceDocument().documentId()))
                .filter(item -> targetDocumentId == null || targetDocumentId.isBlank() || targetDocumentId.equals(item.targetDocument().documentId()))
                .sorted(Comparator.comparing(DocumentRelation::createdTime).reversed())
                .limit(size).toList();
    }

    @Override
    public synchronized List<DocumentRelation> findActive(int limit) {
        return relations.stream().filter(item -> item.status() == RelationStatus.ACTIVE).limit(limit).toList();
    }

    @Override
    public synchronized List<DocumentRelation> invalidateByTarget(
            String tenantId,
            String targetDocumentId,
            String eventId,
            String targetStatus,
            String reason
    ) {
        List<DocumentRelation> changed = new ArrayList<>();
        for (int index = 0; index < relations.size(); index++) {
            DocumentRelation relation = relations.get(index);
            if (!tenantId.equals(relation.tenantId()) || !targetDocumentId.equals(relation.targetDocument().documentId())) {
                continue;
            }
            if (eventId.equals(relation.lastEventId()) || relation.status() != RelationStatus.ACTIVE) {
                continue;
            }
            DocumentRelation updated = copy(relation, RelationStatus.INVALID, targetStatus, eventId, reason, LocalDateTime.now(), null);
            relations.set(index, updated);
            changed.add(updated);
        }
        return changed;
    }

    @Override
    public synchronized DocumentRelation invalidateById(Long relationId, String eventId, String reason) {
        DocumentRelation relation = require(relationId);
        if (eventId.equals(relation.lastEventId()) || relation.status() != RelationStatus.ACTIVE) {
            return relation;
        }
        DocumentRelation updated = copy(relation, RelationStatus.INVALID, "MANUAL_INVALID", eventId, reason, LocalDateTime.now(), null);
        replace(updated);
        return updated;
    }

    @Override
    public synchronized DocumentRelation markReversing(Long relationId) {
        DocumentRelation relation = require(relationId);
        if (relation.status() != RelationStatus.INVALID) {
            return relation;
        }
        DocumentRelation updated = copy(relation, RelationStatus.REVERSING, relation.targetStatus(), relation.lastEventId(), relation.invalidReason(), relation.invalidTime(), null);
        replace(updated);
        return updated;
    }

    @Override
    public synchronized DocumentRelation markReversed(Long relationId) {
        DocumentRelation relation = require(relationId);
        if (relation.status() != RelationStatus.INVALID && relation.status() != RelationStatus.REVERSING) {
            return relation;
        }
        DocumentRelation updated = copy(relation, RelationStatus.REVERSED, relation.targetStatus(), relation.lastEventId(), relation.invalidReason(), relation.invalidTime(), LocalDateTime.now());
        replace(updated);
        return updated;
    }

    private DocumentRelation require(Long relationId) {
        return findById(relationId).orElseThrow(() -> new BizException("BOTP 单据关系不存在: " + relationId));
    }

    private void replace(DocumentRelation relation) {
        for (int index = 0; index < relations.size(); index++) {
            if (relations.get(index).relationId().equals(relation.relationId())) {
                relations.set(index, relation);
                return;
            }
        }
    }

    private DocumentRelation copy(
            DocumentRelation source,
            RelationStatus status,
            String targetStatus,
            String eventId,
            String reason,
            LocalDateTime invalidTime,
            LocalDateTime reversedTime
    ) {
        return new DocumentRelation(
                source.relationId(), source.tenantId(), source.executionId(), source.ruleCode(), source.ruleVersion(),
                source.sourceDocument(), source.targetDocument(), source.allocatedAmount(), status, targetStatus,
                eventId, reason, source.createdTime(), invalidTime, reversedTime
        );
    }
}
