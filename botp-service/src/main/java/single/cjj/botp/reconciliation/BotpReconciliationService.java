package single.cjj.botp.reconciliation;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.ReconciliationActionRequest;
import single.cjj.botp.domain.BotpContracts.ReconciliationIssue;
import single.cjj.botp.domain.BotpContracts.ReconciliationIssueType;
import single.cjj.botp.domain.BotpContracts.TargetStatusEvent;
import single.cjj.botp.domain.BotpContracts.WritebackTask;
import single.cjj.botp.integration.fi.FiArapClient;
import single.cjj.botp.integration.fi.FiArapClientContracts.FiArapDocument;
import single.cjj.botp.relation.BotpRelationLifecycleService;
import single.cjj.botp.relation.BotpRelationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BotpReconciliationService {

    private final BotpRelationRepository relationRepository;
    private final BotpReconciliationRepository issueRepository;
    private final BotpRelationLifecycleService lifecycleService;
    private final FiArapClient fiArapClient;

    public BotpReconciliationService(
            BotpRelationRepository relationRepository,
            BotpReconciliationRepository issueRepository,
            BotpRelationLifecycleService lifecycleService,
            FiArapClient fiArapClient
    ) {
        this.relationRepository = relationRepository;
        this.issueRepository = issueRepository;
        this.lifecycleService = lifecycleService;
        this.fiArapClient = fiArapClient;
    }

    @Scheduled(fixedDelayString = "${botp.reconciliation.scan-interval-ms:3600000}")
    public void scheduledRun() {
        runNow(500, true);
    }

    public List<ReconciliationIssue> runNow(int limit, boolean autoFix) {
        List<ReconciliationIssue> detected = new ArrayList<>();
        for (DocumentRelation relation : relationRepository.findActive(limit)) {
            if (!"MATRIX".equals(relation.sourceDocument().systemCode())
                    || !"FI_AP_DOC".equals(relation.sourceDocument().documentType())) {
                continue;
            }
            FiArapDocument source = requireData(
                    fiArapClient.detail(parseId(relation.sourceDocument().documentId())), "读取对账源单失败");
            BigDecimal expected = relationRepository.sumActiveAmount(relation.tenantId(), relation.sourceDocument());
            BigDecimal actual = nz(source.fappliedAmount());
            if (expected.compareTo(actual) != 0) {
                ReconciliationIssue issue = issueRepository.saveOpen(
                        relation.tenantId(), ReconciliationIssueType.SOURCE_AMOUNT_MISMATCH, relation,
                        expected, actual, "关系台账有效金额与应付单已申请金额不一致");
                detected.add(issue);
                if (autoFix) {
                    WritebackTask task = lifecycleService.recompute(relation.relationId());
                    if (task.status().name().equals("SUCCEEDED")) {
                        issueRepository.markFixed(issue.issueId(), "已按有效关系金额重算反写");
                    }
                }
            }

            FiArapDocument target = requireData(
                    fiArapClient.detail(parseId(relation.targetDocument().documentId())), "读取对账目标单失败");
            if (isInvalidatingStatus(target.fstatus())) {
                ReconciliationIssue issue = issueRepository.saveOpen(
                        relation.tenantId(), ReconciliationIssueType.TARGET_VOID_RELATION_ACTIVE, relation,
                        BigDecimal.ZERO, relation.allocatedAmount(), "目标单已失效但关系仍为 ACTIVE");
                detected.add(issue);
                if (autoFix) {
                    lifecycleService.handleTargetStatusEvent(new TargetStatusEvent(
                            "RECON-" + relation.targetDocument().documentId() + "-" + target.fstatus(),
                            relation.tenantId(), relation.targetDocument().systemCode(), relation.targetDocument().documentType(),
                            relation.targetDocument().documentId(), target.fstatus(), "自动对账发现目标单已失效", "reconciliation", LocalDateTime.now()
                    ));
                    issueRepository.markFixed(issue.issueId(), "已失效关系并执行反向重算");
                }
            }
        }
        return detected;
    }

    public List<ReconciliationIssue> list(int limit) {
        return issueRepository.list(limit);
    }

    public ReconciliationIssue fix(Long issueId, ReconciliationActionRequest request) {
        ReconciliationIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new BizException("BOTP 对账异常不存在: " + issueId));
        DocumentRelation relation = relationRepository.findById(issue.relationId())
                .orElseThrow(() -> new BizException("BOTP 单据关系不存在: " + issue.relationId()));
        if (issue.issueType() == ReconciliationIssueType.SOURCE_AMOUNT_MISMATCH) {
            lifecycleService.recompute(relation.relationId());
        } else if (issue.issueType() == ReconciliationIssueType.TARGET_VOID_RELATION_ACTIVE) {
            lifecycleService.handleTargetStatusEvent(new TargetStatusEvent(
                    "MANUAL-FIX-" + issue.issueId(), relation.tenantId(), relation.targetDocument().systemCode(),
                    relation.targetDocument().documentType(), relation.targetDocument().documentId(), "VOID",
                    request == null ? "人工对账修复" : request.resolution(),
                    request == null ? "system" : request.operator(), LocalDateTime.now()
            ));
        }
        return issueRepository.markFixed(issueId, request == null ? "人工修复" : request.resolution());
    }

    public ReconciliationIssue ignore(Long issueId, ReconciliationActionRequest request) {
        String resolution = request == null || request.resolution() == null || request.resolution().isBlank()
                ? "人工忽略" : request.resolution();
        return issueRepository.markIgnored(issueId, resolution);
    }

    private boolean isInvalidatingStatus(String status) {
        return "VOID".equalsIgnoreCase(status)
                || "CANCELLED".equalsIgnoreCase(status)
                || "DELETED".equalsIgnoreCase(status)
                || "REJECTED".equalsIgnoreCase(status);
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BizException("单据ID格式错误: " + value);
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private FiArapDocument requireData(ApiResponse<FiArapDocument> response, String action) {
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            String message = response == null ? null : response.getMessage();
            throw new BizException(action + (message == null || message.isBlank() ? "" : ": " + message));
        }
        return response.getData();
    }
}
