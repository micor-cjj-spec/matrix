package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldDelegateStreamingToSelectedAdapter() {
        AiModelRequest request = request();
        @SuppressWarnings("unchecked")
        Consumer<String> consumer = mock(Consumer.class);
        AiModelResult expected = result("answer");
        when(promptDrivenFacade.stream(same(request), same(consumer))).thenReturn(expected);

        AiModelResult actual = routingFacade.stream(request, consumer);

        assertSame(expected, actual);
        verify(promptDrivenFacade).stream(same(request), same(consumer));
    }

    @Test
    void shouldFallbackToPromptAdapterWhenSpringAiChatFails() {
        aiProperties.setModelAdapter(RoutingAiModelFacade.ADAPTER_SPRING_AI);
        aiProperties.setFallbackEnabled(true);
        AiModelRequest request = request();
        AiModelResult fallback = result("fallback");
        when(springAiFacade.chat(request)).thenThrow(new IllegalStateException("remote down"));
        when(promptDrivenFacade.chat(request)).thenReturn(fallback);

        assertSame(fallback, routingFacade.chat(request));
    }

    @Test
    void shouldFallbackStreamingOnlyBeforeAnyDeltaWasEmitted() {
        aiProperties.setModelAdapter(RoutingAiModelFacade.ADAPTER_SPRING_AI);
        aiProperties.setFallbackEnabled(true);
        AiModelRequest request = request();
        @SuppressWarnings("unchecked")
        Consumer<String> consumer = mock(Consumer.class);
        AiModelResult fallback = result("fallback");
        doThrow(new IllegalStateException("remote down"))
                .when(springAiFacade).stream(same(request), any());
        when(promptDrivenFacade.stream(same(request), same(consumer))).thenReturn(fallback);

        assertSame(fallback, routingFacade.stream(request, consumer));
    }

    @Test
    void shouldNotFallbackStreamingAfterPartialOutput() {
        aiProperties.setModelAdapter(RoutingAiModelFacade.ADAPTER_SPRING_AI);
        aiProperties.setFallbackEnabled(true);
        AiModelRequest request = request();
        @SuppressWarnings("unchecked")
        Consumer<String> consumer = mock(Consumer.class);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<String> wrapped = invocation.getArgument(1);
            wrapped.accept("partial");
            throw new IllegalStateException("stream interrupted");
        }).when(springAiFacade).stream(same(request), any());

        assertThrows(IllegalStateException.class, () -> routingFacade.stream(request, consumer));
        verify(consumer).accept("partial");
        verify(promptDrivenFacade, never()).stream(any(), any());
    }

    private AiModelRequest request() {
        return new AiModelRequest("question", List.of(), List.of());
    }

    private AiModelResult result(String answer) {
        return new AiModelResult(
                answer,
                "model",
                "real-model",
                "trace",
                0,
                0,
                0,
                0.0
        );
    }
}
