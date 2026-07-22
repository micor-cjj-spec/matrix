package single.cjj.botp.relation;

import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BotpRelationRepository {

    DocumentRelation saveActive(
            String tenantId,
            String executionId,
            RuleDefinition rule,
            DocumentRef source,
            TargetResult target,
            BigDecimal allocatedAmount
    );

    BigDecimal sumActiveAmount(String tenantId, DocumentRef source);

    Optional<DocumentRelation> findById(Long relationId);

    List<DocumentRelation> findByTarget(String tenantId, String targetDocumentId);

    List<DocumentRelation> find(String tenantId, String sourceDocumentId, String targetDocumentId, int limit);

    List<DocumentRelation> findActive(int limit);

    List<DocumentRelation> invalidateByTarget(
            String tenantId,
            String targetDocumentId,
            String eventId,
            String targetStatus,
            String reason
    );

    DocumentRelation invalidateById(Long relationId, String eventId, String reason);

    DocumentRelation markReversing(Long relationId);

    DocumentRelation markReversed(Long relationId);
}
