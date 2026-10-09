package single.cjj.erp.sales.commercial.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    private final RedisTemplate<String, String> loginSessions;
    private final StringRedisTemplate revisions;

    public SalesAccessGuard(@Value("${security.jwt.secret:}") String secret,
                            RedisTemplate<String, String> loginSessions,
                            StringRedisTemplate revisions) {
        this.loginSessions = loginSessions;
        this.revisions = revisions;
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
    public Long authorizeTenant(String bearer, String tenantId, Permission permission) {
        return check(bearer, tenantId, null, permission, false);
    }

    public Long authorize(String bearer, String tenantId, Long orgId, Permission permission) {
        return check(bearer, tenantId, orgId, permission, true);
    }

    private Long check(String bearer, String tenantId, Long orgId,
                       Permission permission, boolean checkOrganization) {
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

        // Auth-service stores only currently live tokens. Checking this on every
        // request also makes explicit session invalidation effective immediately.
        // Redis outage must fail closed, not fall back to accepting a signed JWT.
        try {
            String activeUserId = loginSessions.opsForValue().get("token:" + bearer.substring(7));
            if (!userId.toString().equals(activeUserId)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录会话已失效");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "认证会话服务暂不可用");
        }

        String trustedTenant = String.valueOf(claims.getOrDefault("tenantId", "default"));
        if (!StringUtils.hasText(tenantId) || !trustedTenant.equals(tenantId.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "租户不在授权范围");
        }
        if (checkOrganization && (orgId == null || !hasOrganization(claims.get("organizationIds"), orgId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "组织不在授权范围");
        }

        Set<String> roles = new HashSet<>();
        for (String claimName : List.of("roles", "roleCodes", "authorities", "role")) {
            collectRoles(claims.get(claimName), roles);
        }
        // Every sales JWT must carry the revision of the trusted grant snapshot.
        // An odd revision means a grant is being updated, so authorization fails closed.
        // A changed revision invalidates pre-change JWTs without storing JWT strings.
        final long signedRevision;
        try {
            Object claim = claims.get("salesGrantRevision");
            if (!(claim instanceof Number)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "销售权限版本缺失，请重新登录");
            }
            signedRevision = ((Number) claim).longValue();
        } catch (ResponseStatusException ex) {
            throw ex;
        }
        if (signedRevision < 0 || signedRevision % 2 != 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "销售权限版本异常");
        }
        try {
            Long revisionOrg = null;
            Object orgClaim = claims.get("organizationIds");
            if (orgClaim instanceof Collection<?> values && values.size() == 1) {
                revisionOrg = Long.valueOf(values.iterator().next().toString());
            }
            if (revisionOrg == null || revisionOrg <= 0) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "销售令牌必须绑定唯一组织");
            }
            String encodedTenant = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(trustedTenant.getBytes(StandardCharsets.UTF_8));
            String key = "sales:acl:revision:" + encodedTenant + ":" + revisionOrg + ":" + userId;
            String value = revisions.opsForValue().get(key);
            if (value == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "销售授权版本不存在，请重新登录");
            }
            long currentRevision = Long.parseLong(value);
            if (currentRevision % 2 != 0 || currentRevision != signedRevision) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "销售权限已变更，请重新登录");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "销售授权版本验证不可用");
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
