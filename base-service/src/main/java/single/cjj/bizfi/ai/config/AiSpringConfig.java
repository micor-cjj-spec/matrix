package single.cjj.bizfi.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AiProperties.class, AiVectorStoreProperties.class})
public class AiSpringConfig {
}
