package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiChatRequest;
import single.cjj.bizfi.ai.dto.AiToolContext;
import single.cjj.bizfi.exception.BizException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAiToolPolicyServiceTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIgnoreToolPolicyForNormalChat() {
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(new AiProperties());
        AiChatRequest request = new AiChatRequest();
        request.setTaskType("general");

        assertNull(service.prepareContext(7L, request));
    }

    @Test
    void shouldDenyToolsByDefault() {
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(new AiProperties());
        AiChatRequest request = validRequest();

        assertThrows(BizException.class, () -> service.prepareContext(7L, request));
    }

    @Test
    void shouldRequireSpringAiAdapter() {
        AiProperties properties = enabledProperties();
        properties.setModelAdapter(RoutingAiModelFacade.ADAPTER_PROMPT_HTTP);
        properties.setToolAllowAllOrganizations(true);
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(properties);

        assertThrows(BizException.class, () -> service.prepareContext(7L, validRequest()));
    }

    @Test
    void shouldAuthorizeOrganizationFromSecurityAuthority() {
        AiProperties properties = enabledProperties();
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(properties);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "7",
                "n/a",
                List.of(new SimpleGrantedAuthority("ORG:10"))
        ));

        AiToolContext context = service.prepareContext(7L, validRequest());

        assertEquals(7L, context.getRequestedByUserId());
        assertEquals(10L, context.getOrganizationId());
        assertEquals("2026-07", context.getPeriod());
        assertNotNull(context.getRequestId());
    }

    @Test
    void shouldAuthorizeConfiguredOrganization() {
        AiProperties properties = enabledProperties();
        properties.setToolAllowedOrganizationIds("8, 10,invalid");
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(properties);

        AiToolContext context = service.prepareContext(7L, validRequest());

        assertEquals(10L, context.getOrganizationId());
    }

    @Test
    void shouldRejectInvalidPeriod() {
        AiProperties properties = enabledProperties();
        properties.setToolAllowAllOrganizations(true);
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(properties);
        AiChatRequest request = validRequest();
        request.setAccountingPeriod("2026-13");

        assertThrows(BizException.class, () -> service.prepareContext(7L, request));
    }

    private AiProperties enabledProperties() {
        AiProperties properties = new AiProperties();
        properties.setToolCallingEnabled(true);
        properties.setModelAdapter(RoutingAiModelFacade.ADAPTER_SPRING_AI);
        return properties;
    }

    private AiChatRequest validRequest() {
        AiChatRequest request = new AiChatRequest();
        request.setTaskType("tool-calling");
        request.setToolName("month-end-close-check");
        request.setOrganizationId(10L);
        request.setAccountingPeriod("2026-07");
        return request;
    }
}
