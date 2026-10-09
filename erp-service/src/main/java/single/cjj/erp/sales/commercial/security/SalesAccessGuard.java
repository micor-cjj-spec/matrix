package single.cjj.erp.sales.commercial.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

@Component
@ConditionalOnProperty(prefix = "matrix.sales", name = "commercial-enabled", havingValue = "true")
public class SalesAccessGuard {
    public enum Permission { READ, WRITE, APPROVE }

    private static final Set<String> VIEW_ROLES =
            Set.of("SALES_VIEWER", "SALES_EDITOR", "SALES_APPROVER", "SALES_ADMIN", "ADMIN");
    private static final Set<String> WRITE_ROLES =
            Set.of("SALES_EDITOR", "SALES_ADMIN", "ADMIN");
    private static final Set<String> APPROVE_ROLES =
            Set.of("SALES_APPROVER", "SALES_ADMIN", "ADMIN");

    private final Key verificationKey;

    public SalesAccessGuard(@Value("${security.jwt.secret:}") String secret) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "Sales APIs require security.jwt.secret with at least 32 bytes matching auth-service");
        }
        this.verificationKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Never derive principal, tenant, roles or organization access from forwarded
     * identity headers. Only a correctly signed non-expired bearer token is trusted.
     */
    public Long authorize(String bearer, String tenantId, Long orgId, Permission permission) {
        if (!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录令牌缺失");
        }
        final Claims claims;
        try {
            claims = Jwts.parserBuilder().setSigningKey(verificationKey).build()
                    .parseClaimsJws(bearer.substring(7)).getBody();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录令牌无效");
        }

        final Long userId;
        try {
            userId = Long.valueOf(String.valueOf(claims.get("id")));
            if (userId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "令牌缺少用户身份");
        }

        String trustedTenant = String.valueOf(claims.getOrDefault("tenantId", "default"));
        if (!StringUtils.hasText(tenantId) || !trustedTenant.equals(tenantId.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "租户不在授权范围");
        }
        if (orgId == null || !hasOrganization(claims.get("organizationIds"), orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "组织不在授权范围");
        }

        Set<String> roles = new HashSet<>();
        for (String claimName : List.of("roles", "roleCodes", "authorities", "role")) {
            collectRoles(claims.get(claimName), roles);
        }
        Set<String> allowed = switch (permission) {
            case READ -> VIEW_ROLES;
            case WRITE -> WRITE_ROLES;
            case APPROVE -> APPROVE_ROLES;
        };
        if (roles.stream().noneMatch(allowed::contains)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号缺少销售业务权限");
        }
        return userId;
    }

    private boolean hasOrganization(Object claim, Long requested) {
        if (claim == null) return false;
        if (claim instanceof Collection<?> values) {
            return values.stream().anyMatch(item -> requested.toString().equals(String.valueOf(item)));
        }
        return Arrays.stream(String.valueOf(claim).split(","))
                .anyMatch(item -> requested.toString().equals(item.trim()));
    }

    private void collectRoles(Object claim, Set<String> roles) {
        if (claim == null) return;
        if (claim instanceof Collection<?> values) {
            values.forEach(item -> collectRoles(item, roles));
        } else {
            for (String raw : String.valueOf(claim).split(",")) {
                String role = raw.trim().toUpperCase(Locale.ROOT);
                if (role.startsWith("ROLE_")) role = role.substring(5);
                if (!role.isBlank()) roles.add(role);
            }
        }
    }
}

