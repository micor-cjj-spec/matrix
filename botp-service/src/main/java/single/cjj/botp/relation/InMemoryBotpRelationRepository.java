package single.cjj.botp.relation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
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
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        DocumentRelation relation = new DocumentRelation(
                sequence.getAndIncrement(),
                tenantId,
                executionId,
                rule.ruleCode(),
                rule.version(),
                source,
                target,
                allocatedAmount,
                RelationStatus.ACTIVE,
                LocalDateTime.now()
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
    public synchronized List<DocumentRelation> find(
            String tenantId,
            String sourceDocumentId,
            String targetDocumentId,
            int limit
    ) {
        int size = Math.max(1, Math.min(limit, 200));
        return relations.stream()
                .filter(item -> tenantId == null || tenantId.equals(item.tenantId()))
                .filter(item -> sourceDocumentId == null || sourceDocumentId.isBlank()
                        || sourceDocumentId.equals(item.sourceDocument().documentId()))
                .filter(item -> targetDocumentId == null || targetDocumentId.isBlank()
                        || targetDocumentId.equals(item.targetDocument().documentId()))
                .sorted(Comparator.comparing(DocumentRelation::createdTime).reversed())
                .limit(size)
                .toList();
    }
}
