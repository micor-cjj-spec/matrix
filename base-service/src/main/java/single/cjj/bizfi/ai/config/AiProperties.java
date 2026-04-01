package single.cjj.bizfi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bizfi.ai")
public class AiProperties {

    private Boolean enabled = true;
    private Boolean knowledgeEnabled = true;
    private Boolean fallbackEnabled = true;
    private Integer maxHistoryMessages = 20;
    private Integer maxKnowledgeChunks = 5;
    private Integer requestTimeoutSeconds = 60;
}
