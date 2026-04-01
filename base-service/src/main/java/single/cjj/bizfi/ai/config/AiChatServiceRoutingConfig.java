package single.cjj.bizfi.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import single.cjj.bizfi.ai.service.AiChatService;
import single.cjj.bizfi.ai.service.impl.AiChatEnhancedServiceImpl;

@Configuration
public class AiChatServiceRoutingConfig {

    @Bean
    @Primary
    public AiChatService primaryAiChatService(AiChatEnhancedServiceImpl enhancedService) {
        return enhancedService;
    }
}
