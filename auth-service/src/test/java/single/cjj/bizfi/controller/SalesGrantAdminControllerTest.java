package single.cjj.bizfi.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.bizfi.mapper.SalesRoleGrantMapper;
import single.cjj.bizfi.security.SalesAclRevision;
import single.cjj.bizfi.service.SalesGrantAdminService;
import single.cjj.bizfi.utils.JwtUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalesGrantAdminControllerTest {
    private static final String SECRET = "matrix-test-signing-key-32-bytes-minimum-only";
    SalesGrantAdminService service = mock(SalesGrantAdminService.class);
    SalesRoleGrantMapper grants = mock(SalesRoleGrantMapper.class);
    SalesAclRevision revisions = mock(SalesAclRevision.class);
    @SuppressWarnings("unchecked")
    RedisTemplate<String, String> sessions = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> values = mock(ValueOperations.class);
    SalesGrantAdminController controller = new SalesGrantAdminController(service, grants, revisions, sessions);

    @BeforeAll
    static void initJwt() { new JwtUtils(SECRET); }

    private String bearer(String tenant, Long orgId) {
        String token = Jwts.builder().claim("id", 99L).claim("tenantId", tenant)
                .claim("organizationIds", List.of(orgId))
                .claim("roles", List.of("SALES_ADMIN"))
                .claim("salesGrantRevision", 0L)
                .setExpiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256).compact();
        return "Bearer " + token;
    }

    private String authorizeAdmin() {
        String token = bearer("T1", 3L);
        when(sessions.opsForValue()).thenReturn(values);
        when(values.get("token:" + token.substring(7))).thenReturn("99");
        when(revisions.current("T1", 3L, 99L)).thenReturn(0L);
        when(grants.activeRoles(99L, "T1", 3L)).thenReturn(List.of("SALES_ADMIN"));
        return token;
    }

    @Test
    void authenticatedAdminCanRevokeOnlyWithinHisOrganization() {
        String bearer = authorizeAdmin();
        controller.revoke(bearer, new SalesGrantAdminController.GrantRequest(
                "T1", 3L, 77L, "SALES_EDITOR"));
        verify(service).change("T1", 3L, 77L, "SALES_EDITOR", false, 99L);
        assertEquals(HttpStatus.FORBIDDEN.value(), status(() -> controller.revoke(bearer,
                new SalesGrantAdminController.GrantRequest("T1", 4L, 77L, "SALES_EDITOR"))));
        verifyNoMoreInteractions(service);
    }

    @Test
    void expiredOrMissingSessionCannotManageGrants() {
        String bearer = bearer("T1", 3L);
        when(sessions.opsForValue()).thenReturn(values);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), status(() -> controller.grant(bearer,
                new SalesGrantAdminController.GrantRequest("T1", 3L, 77L, "SALES_EDITOR"))));
        verifyNoInteractions(service);
    }

    @Test
    void signedRoleDoesNotOverrideRevokedDatabaseAdminPermission() {
        String bearer = authorizeAdmin();
        when(grants.activeRoles(99L, "T1", 3L)).thenReturn(List.of());
        assertEquals(HttpStatus.FORBIDDEN.value(), status(() -> controller.grant(bearer,
                new SalesGrantAdminController.GrantRequest("T1", 3L, 77L, "SALES_ADMIN"))));
        verifyNoInteractions(service);
    }

    @Test
    void oldAdminTokenIsDeniedAfterRevisionChanges() {
        String bearer = authorizeAdmin();
        when(revisions.current("T1", 3L, 99L)).thenReturn(2L);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), status(() -> controller.grant(bearer,
                new SalesGrantAdminController.GrantRequest("T1", 3L, 77L, "SALES_VIEWER"))));
        verifyNoInteractions(service);
    }

    private int status(Runnable action) {
        return assertThrows(ResponseStatusException.class, action::run).getStatusCode().value();
    }
}

