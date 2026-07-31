package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.ai.config.AiProperties;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class RoutingAiModelFacadeTest {

    private final AiProperties aiProperties = new AiProperties();
    private final PromptDrivenAiModelFacade promptDrivenFacade = mock(PromptDrivenAiModelFacade.class);
    private final DefaultAiModelFacade defaultFacade = mock(DefaultAiModelFacade.class);
    private final SpringAiModelFacade springAiFacade = mock(SpringAiModelFacade.class);

    private final RoutingAiModelFacade routingFacade = new RoutingAiModelFacade(
            aiProperties,
            promptDrivenFacade,
            defaultFacade,
            springAiFacade
    );

    @Test
    void shouldUsePromptDrivenAdapterByDefault() {
        assertSame(promptDrivenFacade, routingFacade.resolveDelegate());
    }

    @Test
    void shouldRouteToLegacyHttpAdapter() {
        aiProperties.setModelAdapter(RoutingAiModelFacade.ADAPTER_LEGACY_HTTP);

        assertSame(defaultFacade, routingFacade.resolveDelegate());
    }

    @Test
    void shouldRouteToSpringAiAdapter() {
        aiProperties.setModelAdapter(RoutingAiModelFacade.ADAPTER_SPRING_AI);

        assertSame(springAiFacade, routingFacade.resolveDelegate());
    }

    @Test
    void shouldNormalizeAdapterValue() {
        aiProperties.setModelAdapter("  PROMPT-HTTP  ");

        assertSame(promptDrivenFacade, routingFacade.resolveDelegate());
    }

    @Test
    void shouldRejectUnknownAdapter() {
        aiProperties.setModelAdapter("unknown");

        assertThrows(IllegalStateException.class, routingFacade::resolveDelegate);
    }
}
