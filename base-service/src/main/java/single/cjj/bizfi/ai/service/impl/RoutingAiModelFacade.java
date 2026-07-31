package single.cjj.bizfi.ai.service.impl;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;
import single.cjj.bizfi.ai.service.AiModelFacade;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Unified routing entry for AI model adapters.
 */
@Service
@Primary
public class RoutingAiModelFacade implements AiModelFacade {

    public static final String ADAPTER_PROMPT_HTTP = "prompt-http";
    public static final String ADAPTER_LEGACY_HTTP = "legacy-http";
    public static final String ADAPTER_SPRING_AI = "spring-ai";

    private final AiProperties aiProperties;
    private final PromptDrivenAiModelFacade promptDrivenAiModelFacade;
    private final DefaultAiModelFacade defaultAiModelFacade;
    private final SpringAiModelFacade springAiModelFacade;

    public RoutingAiModelFacade(
            AiProperties aiProperties,
            PromptDrivenAiModelFacade promptDrivenAiModelFacade,
            DefaultAiModelFacade defaultAiModelFacade,
            SpringAiModelFacade springAiModelFacade
    ) {
        this.aiProperties = aiProperties;
        this.promptDrivenAiModelFacade = promptDrivenAiModelFacade;
        this.defaultAiModelFacade = defaultAiModelFacade;
        this.springAiModelFacade = springAiModelFacade;
    }

    @Override
    public AiModelResult chat(AiModelRequest request) {
        AiModelFacade delegate = resolveDelegate();
        try {
            return delegate.chat(request);
        } catch (RuntimeException failure) {
            if (!shouldFallback(delegate)) {
                throw failure;
            }
            return promptDrivenAiModelFacade.chat(request);
        }
    }

    @Override
    public AiModelResult stream(AiModelRequest request, Consumer<String> deltaConsumer) {
        AiModelFacade delegate = resolveDelegate();
        if (!shouldFallback(delegate)) {
            return delegate.stream(request, deltaConsumer);
        }

        AtomicBoolean emitted = new AtomicBoolean(false);
        try {
            return delegate.stream(request, delta -> {
                emitted.set(true);
                deltaConsumer.accept(delta);
            });
        } catch (RuntimeException failure) {
            if (emitted.get()) {
                throw failure;
            }
            return promptDrivenAiModelFacade.stream(request, deltaConsumer);
        }
    }

    @Override
    public AiConfigStatusResponse configStatus() {
        return resolveDelegate().configStatus();
    }

    AiModelFacade resolveDelegate() {
        String configuredAdapter = aiProperties.getModelAdapter();
        String adapter = StringUtils.hasText(configuredAdapter)
                ? configuredAdapter.trim().toLowerCase(Locale.ROOT)
                : ADAPTER_PROMPT_HTTP;

        return switch (adapter) {
            case ADAPTER_PROMPT_HTTP -> promptDrivenAiModelFacade;
            case ADAPTER_LEGACY_HTTP -> defaultAiModelFacade;
            case ADAPTER_SPRING_AI -> springAiModelFacade;
            default -> throw new IllegalStateException(
                    "不支持的 AI 模型适配器: " + configuredAdapter
                            + "，可选值: prompt-http, legacy-http, spring-ai"
            );
        };
    }

    private boolean shouldFallback(AiModelFacade delegate) {
        return delegate == springAiModelFacade && Boolean.TRUE.equals(aiProperties.getFallbackEnabled());
    }
}
