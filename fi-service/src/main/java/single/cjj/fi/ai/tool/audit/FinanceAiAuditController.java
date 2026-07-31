package single.cjj.fi.ai.tool.audit;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/internal/ai/audit/tool-executions")
public class FinanceAiAuditController {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

    private final FinanceAiAuditOperatorGuard operatorGuard;
    private final FinanceAiToolAuditService auditService;
    private final FinanceAiAuditAccessLogService accessLogService;
    private final FinanceAiAuditReconciliationCoordinator reconciliationCoordinator;

    public FinanceAiAuditController(
            FinanceAiAuditOperatorGuard operatorGuard,
            FinanceAiToolAuditService auditService,
            FinanceAiAuditAccessLogService accessLogService,
            FinanceAiAuditReconciliationCoordinator reconciliationCoordinator
    ) {
        this.operatorGuard = operatorGuard;
        this.auditService = auditService;
        this.accessLogService = accessLogService;
        this.reconciliationCoordinator = reconciliationCoordinator;
    }

    @GetMapping("/{requestId}")
    public FinanceAiToolExecutionResponse execution(
            @RequestHeader(value = FinanceAiAuditOperatorGuard.TOKEN_HEADER, required = false) String token,
            @RequestHeader(value = FinanceAiAuditOperatorGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @RequestHeader(value = FinanceAiAuditOperatorGuard.OPERATOR_ROLES_HEADER, required = false) String roles,
            @RequestHeader(value = FinanceAiAuditOperatorGuard.ACCESS_REQUEST_ID_HEADER, required = false) String accessRequestId,
            @PathVariable("requestId") String requestId
    ) {
        FinanceAiAuditOperator operator = operatorGuard.requireViewer(token, operatorId, roles, accessRequestId);
        if (!StringUtils.hasText(requestId) || requestId.trim().length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestId 无效");
        }
        String normalizedRequestId = requestId.trim();
        long startedAt = System.nanoTime();
        FinanceAiToolExecutionResponse response;
        try {
            response = auditService.findByRequestId(normalizedRequestId)
                    .map(FinanceAiToolExecutionResponse::from)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "tool execution 不存在"
                    ));
        } catch (RuntimeException failure) {
            accessLogService.recordRequired(
                    operator,
                    FinanceAiAuditAccessLogService.DETAIL,
                    "requestId=" + normalizedRequestId,
                    FinanceAiAuditAccessLogService.FAILED,
                    0,
                    elapsedMillis(startedAt),
                    failure
            );
            throw failure;
        }
        accessLogService.recordRequired(
                operator,
                FinanceAiAuditAccessLogService.DETAIL,
                "requestId=" + normalizedRequestId,
                FinanceAiAuditAccessLogService.SUCCESS,
                1,
                elapsedMillis(startedAt),
                null
        );
        return response;
    }

    @GetMapping
    public FinanceAiToolExecutionPageResponse executions(
            @RequestHeader(value = FinanceAiAuditOperatorGuard.TOKEN_HEADER, required = false) String token,
            @RequestHeader(value = FinanceAiAuditOperatorGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @RequestHeader(value = FinanceAiAuditOperatorGuard.OPERATOR_ROLES_HEADER, required = false) String roles,
            @RequestHeader(value = FinanceAiAuditOperatorGuard.ACCESS_REQUEST_ID_HEADER, required = false) String accessRequestId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "organizationId", required = false) Long organizationId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "modelTraceId", required = false) String modelTraceId,
            @RequestParam(value = "createdFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(value = "createdTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        FinanceAiAuditOperator operator = operatorGuard.requireViewer(token, operatorId, roles, accessRequestId);
        validateQueryText(period, conversationId, modelTraceId);
        FinanceAiToolExecutionQuery query = new FinanceAiToolExecutionQuery(
                userId,
                organizationId,
                trimToNull(period),
                trimToNull(status),
                trimToNull(conversationId),
                trimToNull(modelTraceId),
                createdFrom,
                createdTo,
                page,
                size
        );
        String summary = filterSummary(query);
        long startedAt = System.nanoTime();
        FinanceAiToolExecutionPageResponse response;
        try {
            response = auditService.query(query);
        } catch (IllegalArgumentException failure) {
            accessLogService.recordRequired(
                    operator,
                    FinanceAiAuditAccessLogService.SEARCH,
                    summary,
                    FinanceAiAuditAccessLogService.FAILED,
                    0,
                    elapsedMillis(startedAt),
                    failure
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, failure.getMessage(), failure);
        } catch (RuntimeException failure) {
            accessLogService.recordRequired(
                    operator,
                    FinanceAiAuditAccessLogService.SEARCH,
                    summary,
                    FinanceAiAuditAccessLogService.FAILED,
                    0,
                    elapsedMillis(startedAt),
                    failure
            );
            throw failure;
        }
        accessLogService.recordRequired(
                operator,
                FinanceAiAuditAccessLogService.SEARCH,
                summary,
                FinanceAiAuditAccessLogService.SUCCESS,
                response.items() == null ? 0 : response.items().size(),
                elapsedMillis(startedAt),
                null
        );
        return response;
    }

    @PostMapping("/reconcile-stale")
    public FinanceAiAuditReconciliationResponse reconcileStale(
            @RequestHeader(value = FinanceAiAuditOperatorGuard.TOKEN_HEADER, required = false) String token,
            @RequestHeader(value = FinanceAiAuditOperatorGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @RequestHeader(value = FinanceAiAuditOperatorGuard.OPERATOR_ROLES_HEADER, required = false) String roles,
            @RequestHeader(value = FinanceAiAuditOperatorGuard.ACCESS_REQUEST_ID_HEADER, required = false) String accessRequestId
    ) {
        FinanceAiAuditOperator operator = operatorGuard.requireReconciler(
                token,
                operatorId,
                roles,
                accessRequestId
        );
        return reconciliationCoordinator.reconcile(operator);
    }

    private void validateQueryText(String period, String conversationId, String modelTraceId) {
        if (StringUtils.hasText(period) && !PERIOD_PATTERN.matcher(period.trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "period 格式应为 yyyy-MM");
        }
        if (StringUtils.hasText(conversationId) && conversationId.trim().length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId 过长");
        }
        if (StringUtils.hasText(modelTraceId) && modelTraceId.trim().length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "modelTraceId 过长");
        }
    }

    private String filterSummary(FinanceAiToolExecutionQuery query) {
        List<String> values = new ArrayList<>();
        add(values, "userId", query.userId());
        add(values, "organizationId", query.organizationId());
        add(values, "period", query.period());
        add(values, "status", query.status());
        add(values, "conversationId", query.conversationId());
        add(values, "modelTraceId", query.modelTraceId());
        add(values, "createdFrom", query.createdFrom());
        add(values, "createdTo", query.createdTo());
        add(values, "page", query.page());
        add(values, "size", query.size());
        return String.join(";", values);
    }

    private void add(List<String> values, String key, Object value) {
        if (value != null && StringUtils.hasText(value.toString())) {
            values.add(key + "=" + value);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - startedAtNanos));
    }
}
