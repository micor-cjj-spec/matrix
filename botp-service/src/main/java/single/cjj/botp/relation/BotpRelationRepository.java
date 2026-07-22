package single.cjj.botp.relation;

import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetResult;

import java.math.BigDecimal;
import java.util.List;

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

    List<DocumentRelation> find(String tenantId, String sourceDocumentId, String targetDocumentId, int limit);
}
