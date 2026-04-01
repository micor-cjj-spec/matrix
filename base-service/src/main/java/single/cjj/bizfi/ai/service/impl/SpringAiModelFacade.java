package single.cjj.bizfi.ai.service.impl;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;
import single.cjj.bizfi.ai.service.AiModelFacade;

/**
 * Spring AI 正式接入占位类。
 *
 * 当前仓库尚未补充 Spring AI 依赖与 Bean 装配时，默认仍使用 DefaultAiModelFacade。
 * 当后续补齐 spring-ai 依赖、ChatModel / ChatClient Bean 后，可将该类切换为正式实现。
 */
@Service("springAiModelFacadePlaceholder")
public class SpringAiModelFacade implements AiModelFacade {

    @Override
    public AiModelResult chat(AiModelRequest request) {
        throw new UnsupportedOperationException("Spring AI 依赖尚未接入，当前请先使用 DefaultAiModelFacade");
    }

    @Override
    public AiConfigStatusResponse configStatus() {
        return new AiConfigStatusResponse(false, "spring-ai-not-ready", "placeholder");
    }
}
