package single.cjj.fi.ai.tool.audit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.fi.ai.tool.FinanceAiToolProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class FinanceAiAuditOperatorGuard {

    public static final String TOKEN_HEADER = "X-Matrix-AI-Audit-Token";
    public static final String OPERATOR_ID_HEADER = "X-Matrix-Audit-Operator-Id";
    public static final String OPERATOR_ROLES_HEADER = "X-Matrix-Audit-Operator-Roles";
    public static final String ACCESS_REQUEST_ID_HEADER = "X-Matrix-Audit-Request-Id";

    public static final String VIEW_ROLE = "AI_TOOL_AUDIT_VIEW";
    public static final String RECONCILE_ROLE = "AI_TOOL_AUDIT_RECONCILE";
    public static final String SYSTEM_RECONCILER_ROLE = "SYSTEM_RECONCILER";

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    private final FinanceAiToolProperties properties;

    public FinanceAiAuditOperatorGuard(FinanceAiToolProperties properties) {
        this.properties = properties;
    }

    public FinanceAiAuditOperator requireViewer(
            String token,
            String operatorId,
            String roles,
            String accessRequestId
    ) {
        FinanceAiAuditOperator operator = verify(token, operatorId, roles, accessRequestId);
        if (!operator.roles().contains(VIEW_ROLE) && !operator.roles().contains(RECONCILE_ROLE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少 AI 工具审计查看权限");
        }
        return operator;
    }

    public FinanceAiAuditOperator requireReconciler(
            String token,
            String operatorId,
            String roles,
            String accessRequestId
    ) {
        FinanceAiAuditOperator operator = verify(token, operatorId, roles, accessRequestId);
        if (!operator.roles().contains(RECONCILE_ROLE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少 AI 工具审计对账权限");
        }
        return operator;
    }

    public FinanceAiAuditOperator systemReconciler(String accessRequestId) {
        return new FinanceAiAuditOperator(
                "system",
                Set.of(SYSTEM_RECONCILER_ROLE),
                normalizeIdentifier(accessRequestId, "accessRequestId")
        );
    }

    private FinanceAiAuditOperator verify(
            String providedToken,
            String operatorId,
            String roles,
            String accessRequestId
    ) {
        verifyToken(providedToken);
        String normalizedOperatorId = normalizeIdentifier(operatorId, "operatorId");
        String normalizedRequestId = normalizeIdentifier(accessRequestId, "accessRequestId");
        Set<String> normalizedRoles = normalizeRoles(roles);
        if (normalizedRoles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "operator roles 不能为空");
        }
        return new FinanceAiAuditOperator(normalizedOperatorId, normalizedRoles, normalizedRequestId);
    }

    private void verifyToken(String providedToken) {
        String configuredToken = properties.getAuditInternalToken();
        if (!StringUtils.hasText(configuredToken)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "FINANCE_AI_AUDIT_INTERNAL_TOKEN 未配置"
            );
        }
        if (!StringUtils.hasText(providedToken)
                || !MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "finance AI audit token 无效");
        }
    }

    private Set<String> normalizeRoles(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .filter(role -> role.length() <= 64)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalizeIdentifier(String value, String field) {
        String normalized = StringUtils.hasText(value) ? value.trim() : null;
        if (normalized == null || !SAFE_IDENTIFIER.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 无效");
        }
        return normalized;
    }
}
