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
import java.util.function.Consumer;

/**
 * AI 模型适配器统一路由入口。
 *
 * <p>业务层只依赖 {@link AiModelFacade}，具体模型调用实现由
 * {@code bizfi.ai.model-adapter} 配置选择，避免多个 {@code @Primary}
 * Bean 和运行时删除 BeanDefinition 的隐式行为。</p>
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
        return resolveDelegate().chat(request);
    }

    @Override
    public AiModelResult stream(AiModelRequest request, Consumer<String> deltaConsumer) {
        return resolveDelegate().stream(request, deltaConsumer);
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
}
