package single.cjj.erp.sales.commercial.security;

import io.jsonwebtoken.Jwts;
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
import static single.cjj.erp.sales.commercial.security.SalesAccessGuard.Permission.*;

class SalesAccessGuardTest {
    private static final String SECRET = "matrix-test-signing-key-32-bytes-minimum-only";
    private final SalesAccessGuard guard = new SalesAccessGuard(SECRET);

    private String bearer(String tenant, List<String> roles, List<Long> organizations) {
        return "Bearer " + Jwts.builder()
                .claim("id", 789L)
                .claim("tenantId", tenant)
                .claim("roles", roles)
                .claim("organizationIds", organizations)
                .setExpiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
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
    void defaultSecretMustNotStartProtectedModule() {
        assertThrows(IllegalStateException.class, () -> new SalesAccessGuard(""));
        assertThrows(IllegalStateException.class, () -> new SalesAccessGuard("short"));
    }

    private int status(Runnable code) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, code::run);
        return ex.getStatusCode().value();
    }
}
