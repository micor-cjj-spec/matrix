package single.cjj.bizfi.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import single.cjj.bizfi.ai.service.AiModelFacade;
import single.cjj.bizfi.ai.service.impl.DefaultAiModelFacade;

@Configuration
public class AiModelFacadeRoutingConfig {

    /**
     * 当前默认走手写 HTTP 实现，保证现有功能可运行。
     * 后续真正接入 Spring AI 后，可在这里切换为 SpringAiModelFacade。
     */
    @Bean
    @Primary
    public AiModelFacade primaryAiModelFacade(DefaultAiModelFacade defaultAiModelFacade) {
        return defaultAiModelFacade;
    }
}
