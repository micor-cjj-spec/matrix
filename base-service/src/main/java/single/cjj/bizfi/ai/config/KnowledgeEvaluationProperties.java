package single.cjj.bizfi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bizfi.ai.knowledge-evaluation")
public class KnowledgeEvaluationProperties {

    private Boolean enabled = false;
    private Long pollDelayMs = 10000L;
    private Integer batchSize = 1;
    private Integer maxCasesPerSet = 100;
    private Integer staleRunningMinutes = 30;
}
