package single.cjj.bizfi.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.mapper.SalesRoleGrantMapper;
import single.cjj.bizfi.security.SalesAclRevision;
import single.cjj.bizfi.service.SalesGrantAdminService;
import single.cjj.bizfi.utils.JwtUtils;

import java.util.*;

/**
 * Deliberately disabled by default. The /auth/** gateway path is public;
 * every method MUST authenticate a signed JWT, live session and active DB
 * SALES_ADMIN grant. Bootstrap first admin only via controlled DBA workflow.
 */
@RestController
@RequestMapping("/auth/sales-role-grants")
@ConditionalOnProperty(prefix="matrix.sales-auth",name="admin-enabled",havingValue="true")
public class SalesGrantAdminController {
    private final SalesGrantAdminService service;
    private final SalesRoleGrantMapper grants;
    private final SalesAclRevision revisions;
    private final RedisTemplate<String, String> sessions;

    public SalesGrantAdminController(SalesGrantAdminService service, SalesRoleGrantMapper grants,
                                     SalesAclRevision revisions, RedisTemplate<String, String> sessions) {
        this.service = service; this.grants = grants; this.revisions = revisions; this.sessions = sessions;
    }

    public record GrantRequest(String tenantId, Long orgId, Long userId, String role) {}

    @PostMapping
    public ApiResponse<String> grant(@RequestHeader(value="Authorization",required=false) String bearer,
                                     @RequestBody GrantRequest request) {
        long operator = requireAdmin(bearer, request.tenantId(), request.orgId());
        service.change(request.tenantId(), request.orgId(), request.userId(),
                request.role(), true, operator);
        return ApiResponse.success("销售角色授权已更新，旧权限令牌立即失效");
    }

    @DeleteMapping
    public ApiResponse<String> revoke(@RequestHeader(value="Authorization",required=false) String bearer,
                                      @RequestBody GrantRequest request) {
        long operator = requireAdmin(bearer, request.tenantId(), request.orgId());
        service.change(request.tenantId(), request.orgId(), request.userId(),
                request.role(), false, operator);
        return ApiResponse.success("销售角色授权已撤销，旧权限令牌立即失效");
    }

    @GetMapping
    public ApiResponse<List<String>> list(@RequestHeader(value="Authorization",required=false) String bearer,
                                          @RequestParam String tenantId, @RequestParam Long orgId,
                                          @RequestParam Long userId) {
        requireAdmin(bearer, tenantId, orgId);
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId must be positive");
        return ApiResponse.success(grants.activeRoles(userId, tenantId, orgId));
    }

    private long requireAdmin(String bearer, String tenantId, Long orgId) {
        if (!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少管理员令牌");
        }
        Claims claims;
        try {
            claims = JwtUtils.parseToken(bearer.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无效的管理员令牌");
        }
        final long user;
        try {
            user = Long.parseLong(String.valueOf(claims.get("id")));
            if (user <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "管理员身份缺失");
        }
        if (!StringUtils.hasText(tenantId)
                || !tenantId.equals(claims.get("tenantId")) || orgId == null || orgId <= 0
                || !(claims.get("organizationIds") instanceof Collection<?> orgs)
                || orgs.stream().noneMatch(x -> orgId.toString().equals(String.valueOf(x)))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能管理其他租户或组织");
        }
        try {
            String active = sessions.opsForValue().get("token:" + bearer.substring(7));
            if (!Long.toString(user).equals(active)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "管理员会话已失效");
            }
            Object version = claims.get("salesGrantRevision");
            long current = revisions.current(tenantId, orgId, user);
            if (!(version instanceof Number number) || current % 2 != 0
                    || number.longValue() != current) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "管理员授权已经变更，请重新登录");
            }
            // DB authority is the source of truth for admin right.
            if (!grants.activeRoles(user, tenantId, orgId).contains("SALES_ADMIN")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号没有销售授权管理权限");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "权限服务不可用");
        }
        return user;
    }
}
