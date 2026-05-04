package single.cjj.bizfi.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import single.cjj.bizfi.ai.service.AiKnowledgeService;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeManagementServiceImpl;

@Configuration
public class AiKnowledgeServiceRoutingConfig {

    @Bean
    @Primary
    public AiKnowledgeService primaryAiKnowledgeService(AiKnowledgeManagementServiceImpl managementService) {
        return managementService;
    }
}
