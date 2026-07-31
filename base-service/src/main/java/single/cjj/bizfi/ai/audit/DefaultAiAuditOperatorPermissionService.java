package single.cjj.bizfi.ai.audit;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.bizfi.ai.config.AiProperties;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DefaultAiAuditOperatorPermissionService implements AiAuditOperatorPermissionService {

    public static final String VIEW_ROLE = "AI_TOOL_AUDIT_VIEW";
    public static final String RECONCILE_ROLE = "AI_TOOL_AUDIT_RECONCILE";

    private final AiProperties properties;

    public DefaultAiAuditOperatorPermissionService(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiAuditOperatorContext requireViewer() {
        ResolvedOperator operator = resolveOperator();
        boolean reconcile = hasAuthority(operator.authorities(), RECONCILE_ROLE)
                || configuredUserIds(properties.getAuditReconcilerUserIds()).contains(operator.userId());
        boolean view = reconcile
                || hasAuthority(operator.authorities(), VIEW_ROLE)
                || configuredUserIds(properties.getAuditViewerUserIds()).contains(operator.userId());
        if (!view) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户没有 AI 工具审计查看权限");
        }
        return context(operator.userId(), true, reconcile);
    }

    @Override
    public AiAuditOperatorContext requireReconciler() {
        ResolvedOperator operator = resolveOperator();
        boolean reconcile = hasAuthority(operator.authorities(), RECONCILE_ROLE)
                || configuredUserIds(properties.getAuditReconcilerUserIds()).contains(operator.userId());
        if (!reconcile) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户没有 AI 工具审计对账权限");
        }
        return context(operator.userId(), true, true);
    }

    private AiAuditOperatorContext context(Long userId, boolean view, boolean reconcile) {
        Set<String> roles = new LinkedHashSet<>();
        if (view) {
            roles.add(VIEW_ROLE);
        }
        if (reconcile) {
            roles.add(RECONCILE_ROLE);
        }
        return new AiAuditOperatorContext(userId, Set.copyOf(roles));
    }

    private ResolvedOperator resolveOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Long userId = parseUserId(authentication.getPrincipal());
        Set<String> authorities = authentication.getAuthorities() == null
                ? Set.of()
                : authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        return new ResolvedOperator(userId, authorities);
    }

    private Long parseUserId(Object principal) {
        String value = principal == null ? null : principal.toString();
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录用户无效");
        }
        try {
            long userId = Long.parseLong(value.trim());
            if (userId <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return userId;
        } catch (NumberFormatException failure) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录用户ID无效", failure);
        }
    }

    private boolean hasAuthority(Set<String> authorities, String role) {
        return authorities.contains(role) || authorities.contains("ROLE_" + role);
    }

    private Set<Long> configuredUserIds(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::parseConfiguredUserId)
                .filter(userId -> userId != null && userId > 0)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Long parseConfiguredUserId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record ResolvedOperator(Long userId, Set<String> authorities) {
    }
}
