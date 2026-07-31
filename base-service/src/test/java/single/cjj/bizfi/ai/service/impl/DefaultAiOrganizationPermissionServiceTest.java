package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.exception.BizException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAiOrganizationPermissionServiceTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthorizeOrganizationAuthority() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "7",
                "n/a",
                List.of(new SimpleGrantedAuthority("ORG:10"))
        ));
        DefaultAiOrganizationPermissionService service = new DefaultAiOrganizationPermissionService(
                new AiProperties()
        );

        assertDoesNotThrow(() -> service.assertCanAccess(7L, 10L));
    }

    @Test
    void shouldAuthorizeExactConfiguredUserOrganizationPair() {
        AiProperties properties = new AiProperties();
        properties.setToolAllowedUserOrganizationPairs("8:10, 7:10, invalid");
        DefaultAiOrganizationPermissionService service = new DefaultAiOrganizationPermissionService(properties);

        assertDoesNotThrow(() -> service.assertCanAccess(7L, 10L));
    }

    @Test
    void shouldRejectPairOwnedByAnotherUser() {
        AiProperties properties = new AiProperties();
        properties.setToolAllowedUserOrganizationPairs("8:10");
        DefaultAiOrganizationPermissionService service = new DefaultAiOrganizationPermissionService(properties);

        assertThrows(BizException.class, () -> service.assertCanAccess(7L, 10L));
    }

    @Test
    void shouldFailClosedWithoutPermissionEvidence() {
        DefaultAiOrganizationPermissionService service = new DefaultAiOrganizationPermissionService(
                new AiProperties()
        );

        assertThrows(BizException.class, () -> service.assertCanAccess(7L, 10L));
    }

    @Test
    void shouldAllowAllOnlyWhenExplicitlyEnabled() {
        AiProperties properties = new AiProperties();
        properties.setToolAllowAllOrganizations(true);
        DefaultAiOrganizationPermissionService service = new DefaultAiOrganizationPermissionService(properties);

        assertDoesNotThrow(() -> service.assertCanAccess(7L, 10L));
    }
}
