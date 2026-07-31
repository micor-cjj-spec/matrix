package single.cjj.bizfi.ai.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.bizfi.ai.config.AiProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAiAuditOperatorPermissionServiceTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthorizeViewerAuthority() {
        authenticate(7L, List.of(new SimpleGrantedAuthority("AI_TOOL_AUDIT_VIEW")));
        DefaultAiAuditOperatorPermissionService service = service(new AiProperties());

        AiAuditOperatorContext operator = service.requireViewer();

        assertEquals(7L, operator.userId());
        assertTrue(operator.roles().contains(DefaultAiAuditOperatorPermissionService.VIEW_ROLE));
    }

    @Test
    void shouldUseConfiguredUserIdsUntilJwtRolesAreAvailable() {
        authenticate(8L, List.of());
        AiProperties properties = new AiProperties();
        properties.setAuditViewerUserIds("7,8,invalid");
        DefaultAiAuditOperatorPermissionService service = service(properties);

        assertEquals(8L, service.requireViewer().userId());
    }

    @Test
    void shouldAllowReconcilerToViewAndReconcile() {
        authenticate(9L, List.of());
        AiProperties properties = new AiProperties();
        properties.setAuditReconcilerUserIds("9");
        DefaultAiAuditOperatorPermissionService service = service(properties);

        AiAuditOperatorContext viewer = service.requireViewer();
        AiAuditOperatorContext reconciler = service.requireReconciler();

        assertTrue(viewer.roles().contains(DefaultAiAuditOperatorPermissionService.RECONCILE_ROLE));
        assertTrue(reconciler.roles().contains(DefaultAiAuditOperatorPermissionService.RECONCILE_ROLE));
    }

    @Test
    void shouldRejectViewerForReconciliation() {
        authenticate(10L, List.of());
        AiProperties properties = new AiProperties();
        properties.setAuditViewerUserIds("10");
        DefaultAiAuditOperatorPermissionService service = service(properties);

        assertThrows(ResponseStatusException.class, service::requireReconciler);
    }

    private DefaultAiAuditOperatorPermissionService service(AiProperties properties) {
        return new DefaultAiAuditOperatorPermissionService(properties);
    }

    private void authenticate(Long userId, List<SimpleGrantedAuthority> authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), "n/a", authorities)
        );
    }
}
