package single.cjj.botp.reconciliation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.ReconciliationIssue;
import single.cjj.botp.domain.BotpContracts.ReconciliationIssueType;
import single.cjj.botp.domain.BotpContracts.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "memory")
public class InMemoryBotpReconciliationRepository implements BotpReconciliationRepository {

    private final AtomicLong sequence = new AtomicLong(1);
    private final List<ReconciliationIssue> issues = new ArrayList<>();

    @Override
    public synchronized ReconciliationIssue saveOpen(
            String tenantId,
            ReconciliationIssueType issueType,
            DocumentRelation relation,
            BigDecimal expectedAmount,
            BigDecimal actualAmount,
            String description
    ) {
        ReconciliationIssue existing = issues.stream()
                .filter(item -> item.status() == ReconciliationStatus.OPEN)
                .filter(item -> item.issueType() == issueType)
                .filter(item -> relation.relationId().equals(item.relationId()))
                .findFirst().orElse(null);
        if (existing != null) {
            ReconciliationIssue updated = copy(existing, ReconciliationStatus.OPEN, description, null, null, expectedAmount, actualAmount);
            replace(updated);
            return updated;
        }
        ReconciliationIssue issue = new ReconciliationIssue(
                sequence.getAndIncrement(), tenantId, issueType, ReconciliationStatus.OPEN,
                relation.executionId(), relation.relationId(), relation.sourceDocument(), relation.targetDocument(),
                expectedAmount, actualAmount, description, null, LocalDateTime.now(), null
        );
        issues.add(issue);
        return issue;
    }

    @Override
    public synchronized Optional<ReconciliationIssue> findById(Long issueId) {
        return issues.stream().filter(item -> item.issueId().equals(issueId)).findFirst();
    }

    @Override
    public synchronized List<ReconciliationIssue> list(int limit) {
        return issues.stream()
                .sorted(Comparator.comparing(ReconciliationIssue::detectedTime).reversed())
                .limit(Math.max(1, limit)).toList();
    }

    @Override
    public synchronized ReconciliationIssue markFixed(Long issueId, String resolution) {
        return resolve(issueId, ReconciliationStatus.FIXED, resolution);
    }

    @Override
    public synchronized ReconciliationIssue markIgnored(Long issueId, String resolution) {
        return resolve(issueId, ReconciliationStatus.IGNORED, resolution);
    }

    private ReconciliationIssue resolve(Long issueId, ReconciliationStatus status, String resolution) {
        ReconciliationIssue issue = findById(issueId)
                .orElseThrow(() -> new BizException("BOTP 对账异常不存在: " + issueId));
        ReconciliationIssue updated = copy(issue, status, issue.description(), resolution, LocalDateTime.now(), issue.expectedAmount(), issue.actualAmount());
        replace(updated);
        return updated;
    }

    private void replace(ReconciliationIssue updated) {
        for (int index = 0; index < issues.size(); index++) {
            if (issues.get(index).issueId().equals(updated.issueId())) {
                issues.set(index, updated);
                return;
            }
        }
    }

    private ReconciliationIssue copy(
            ReconciliationIssue source,
            ReconciliationStatus status,
            String description,
            String resolution,
            LocalDateTime resolvedTime,
            BigDecimal expected,
            BigDecimal actual
    ) {
        return new ReconciliationIssue(
                source.issueId(), source.tenantId(), source.issueType(), status, source.executionId(), source.relationId(),
                source.sourceDocument(), source.targetDocument(), expected, actual, description, resolution,
                source.detectedTime(), resolvedTime
        );
    }
}
