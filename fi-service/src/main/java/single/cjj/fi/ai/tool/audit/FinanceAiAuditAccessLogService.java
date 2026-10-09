package single.cjj.fi.ai.tool.audit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;

@Service
public class FinanceAiAuditAccessLogService {

    public static final String DETAIL = "DETAIL";
    public static final String SEARCH = "SEARCH";
    public static final String RECONCILE = "RECONCILE";

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private final FinanceAiAuditAccessLogMapper mapper;

    public FinanceAiAuditAccessLogService(FinanceAiAuditAccessLogMapper mapper) {
        this.mapper = mapper;
    }

    public void recordRequired(
            FinanceAiAuditOperator operator,
            String action,
            String filterSummary,
            String outcome,
            long resultCount,
            long durationMillis,
            Throwable failure
    ) {
        try {
            FinanceAiAuditAccessLog record = new FinanceAiAuditAccessLog();
            record.setFaccessrequestid(operator.accessRequestId());
            record.setFoperatorid(truncate(operator.operatorId(), 64));
            record.setFoperatorroles(truncate(normalizedRoles(operator), 256));
            record.setFaction(truncate(action, 32));
            record.setFfiltersummary(truncate(sanitize(filterSummary), 500));
            record.setFoutcome(truncate(outcome, 32));
            record.setFresultcount(Math.max(0L, resultCount));
            record.setFdurationms(Math.max(0L, durationMillis));
            record.setFerrorcode(failure == null ? null : errorCode(failure));
            record.setFcreatetime(LocalDateTime.now());
            mapper.insert(record);
        } catch (RuntimeException persistenceFailure) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 审计访问日志不可用，已拒绝返回审计数据",
                    persistenceFailure
            );
        }
    }

    private String normalizedRoles(FinanceAiAuditOperator operator) {
        return operator.roles().stream()
                .sorted(Comparator.naturalOrder())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String errorCode(Throwable failure) {
        String value = failure.getClass().getSimpleName();
        return StringUtils.hasText(value) ? truncate(value, 64) : "UNKNOWN";
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return "none";
        }
        return value.trim().replace('\r', ' ').replace('\n', ' ');
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
