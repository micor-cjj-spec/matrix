package single.cjj.botp.reconciliation;

import single.cjj.botp.domain.BotpContracts.ReconciliationIssue;
import single.cjj.botp.domain.BotpContracts.ReconciliationIssueType;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BotpReconciliationRepository {

    ReconciliationIssue saveOpen(
            String tenantId,
            ReconciliationIssueType issueType,
            DocumentRelation relation,
            BigDecimal expectedAmount,
            BigDecimal actualAmount,
            String description
    );

    Optional<ReconciliationIssue> findById(Long issueId);

    List<ReconciliationIssue> list(int limit);

    ReconciliationIssue markFixed(Long issueId, String resolution);

    ReconciliationIssue markIgnored(Long issueId, String resolution);
}
