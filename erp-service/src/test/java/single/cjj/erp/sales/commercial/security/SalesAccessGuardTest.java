package single.cjj.erp.sales.commercial.security;

import io.jsonwebtoken.Jwts;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static single.cjj.erp.sales.commercial.security.SalesAccessGuard.Permission.*;

class SalesAccessGuardTest {
    private static final String SECRET = "matrix-test-signing-key-32-bytes-minimum-only";
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> sessions = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final SalesAccessGuard guard = new SalesAccessGuard(SECRET, sessions);

    private String bearer(String tenant, List<String> roles, List<Long> organizations) {
        String token = Jwts.builder()
                .claim("id", 789L)
                .claim("tenantId", tenant)
                .claim("roles", roles)
                .claim("organizationIds", organizations)
                .setExpiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
        when(sessions.opsForValue()).thenReturn(values);
        when(values.get("token:" + token)).thenReturn("789");
        return "Bearer " + token;
    }

    @Test
    void tokenMustBeSignedAndNotExpired() {
        assertEquals(HttpStatus.UNAUTHORIZED.value(), status(() ->
                guard.authorize("Bearer invalid", "T1", 3L, READ)));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), status(() ->
                guard.authorize(null, "T1", 3L, READ)));
    }

    @Test
    void tenantAndOrganizationAreBoundToTokenNotInputHeaders() {
        String token = bearer("T1", List.of("SALES_EDITOR"), List.of(3L));
        assertEquals(789L, guard.authorize(token, "T1", 3L, WRITE));
        assertEquals(HttpStatus.FORBIDDEN.value(), status(() ->
                guard.authorize(token, "T2", 3L, READ)));
        assertEquals(HttpStatus.FORBIDDEN.value(), status(() ->
                guard.authorize(token, "T1", 999L, READ)));
        assertEquals(HttpStatus.FORBIDDEN.value(), status(() ->
                guard.authorize(token, "T1", null, READ)));
    }

    @Test
    void editorCannotApproveButSalesApproverCan() {
        String editor = bearer("T1", List.of("SALES_EDITOR"), List.of(3L));
        assertEquals(HttpStatus.FORBIDDEN.value(), status(() ->
                guard.authorize(editor, "T1", 3L, APPROVE)));
        String approver = bearer("T1", List.of("SALES_APPROVER"), List.of(3L));
        assertEquals(789L, guard.authorize(approver, "T1", 3L, APPROVE));
        assertEquals(HttpStatus.FORBIDDEN.value(), status(() ->
                guard.authorize(approver, "T1", 3L, WRITE)));
    }

    @Test
    void legacyRolelessJwtIsFailClosedForAllSalesPermissions() {
        String token = bearer("default", List.of(), List.of(3L));
        assertEquals(HttpStatus.FORBIDDEN.value(), status(() ->
                guard.authorize(token, "default", 3L, READ)));
    }

    @Test
    void untrustedTenantMustFailBeforeDocumentLookup() {
        String token = bearer("T1", List.of("SALES_EDITOR"), List.of(3L));
        assertEquals(789L, guard.authorizeTenant(token, "T1", WRITE));
        assertEquals(HttpStatus.FORBIDDEN.value(), status(() ->
                guard.authorizeTenant(token, "T2", WRITE)));
    }

    @Test
    void invalidatedRedisSessionMustDenyPreviouslySignedJwtImmediately() {
        String token = bearer("T1", List.of("SALES_EDITOR"), List.of(3L));
        assertEquals(789L, guard.authorize(token, "T1", 3L, WRITE));
        when(values.get("token:" + token.substring(7))).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), status(() ->
                guard.authorize(token, "T1", 3L, WRITE)));
    }

    @Test
    void authenticationStoreOutageMustNotFallBackToSignatureOnly() {
        String token = bearer("T1", List.of("SALES_EDITOR"), List.of(3L));
        when(values.get("token:" + token.substring(7)))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), status(() ->
                guard.authorize(token, "T1", 3L, READ)));
    }

    @Test
    void defaultSecretMustNotStartProtectedModule() {
        assertThrows(IllegalStateException.class, () -> new SalesAccessGuard("", sessions));
        assertThrows(IllegalStateException.class, () -> new SalesAccessGuard("short", sessions));
    }

    private int status(Runnable code) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, code::run);
        return ex.getStatusCode().value();
    }
}
