package single.cjj.bizfi.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import single.cjj.bizfi.ai.service.AiModelFacade;
import single.cjj.bizfi.ai.service.impl.PromptDrivenAiModelFacade;

@Configuration
public class AiPromptDrivenModelRoutingConfig {

    @Bean
    @Primary
    public AiModelFacade promptPrimaryAiModelFacade(PromptDrivenAiModelFacade promptDrivenAiModelFacade) {
        return promptDrivenAiModelFacade;
    }
}
