package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiChatRequest;
import single.cjj.bizfi.ai.dto.AiToolContext;
import single.cjj.bizfi.ai.service.AiOrganizationPermissionService;
import single.cjj.bizfi.exception.BizException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DefaultAiToolPolicyServiceTest {

    @Test
    void shouldIgnoreToolPolicyForNormalChat() {
        AiOrganizationPermissionService permissionService = mock(AiOrganizationPermissionService.class);
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(new AiProperties(), permissionService);
        AiChatRequest request = new AiChatRequest();
        request.setTaskType("general");

        assertNull(service.prepareContext(7L, request));
        verify(permissionService, never()).assertCanAccess(7L, 10L);
    }

    @Test
    void shouldDenyToolsByDefault() {
        AiOrganizationPermissionService permissionService = mock(AiOrganizationPermissionService.class);
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(new AiProperties(), permissionService);

        assertThrows(BizException.class, () -> service.prepareContext(7L, validRequest()));
        verify(permissionService, never()).assertCanAccess(7L, 10L);
    }

    @Test
    void shouldRequireSpringAiAdapter() {
        AiProperties properties = enabledProperties();
        properties.setModelAdapter(RoutingAiModelFacade.ADAPTER_PROMPT_HTTP);
        AiOrganizationPermissionService permissionService = mock(AiOrganizationPermissionService.class);
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(properties, permissionService);

        assertThrows(BizException.class, () -> service.prepareContext(7L, validRequest()));
        verify(permissionService, never()).assertCanAccess(7L, 10L);
    }

    @Test
    void shouldDelegateOrganizationAuthorizationAndCreateContext() {
        AiOrganizationPermissionService permissionService = mock(AiOrganizationPermissionService.class);
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(
                enabledProperties(),
                permissionService
        );

        AiToolContext context = service.prepareContext(7L, validRequest());

        verify(permissionService).assertCanAccess(7L, 10L);
        assertEquals(7L, context.getRequestedByUserId());
        assertEquals(10L, context.getOrganizationId());
        assertEquals("2026-07", context.getPeriod());
        assertNotNull(context.getRequestId());
    }

    @Test
    void shouldRejectInvalidPeriodBeforePermissionLookup() {
        AiOrganizationPermissionService permissionService = mock(AiOrganizationPermissionService.class);
        DefaultAiToolPolicyService service = new DefaultAiToolPolicyService(
                enabledProperties(),
                permissionService
        );
        AiChatRequest request = validRequest();
        request.setAccountingPeriod("2026-13");

        assertThrows(BizException.class, () -> service.prepareContext(7L, request));
        verify(permissionService, never()).assertCanAccess(7L, 10L);
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
