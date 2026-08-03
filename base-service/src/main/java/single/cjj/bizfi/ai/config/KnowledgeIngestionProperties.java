package single.cjj.bizfi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bizfi.ai.knowledge-ingestion")
public class KnowledgeIngestionProperties {

    private Boolean enabled = false;
    private Long maxFileSizeBytes = 10L * 1024L * 1024L;
    private Integer maxExtractedCharacters = 2_000_000;
    private Integer maxAttempts = 3;
    private Integer batchSize = 5;
    private Long pollDelayMs = 5_000L;
    private Integer staleRunningMinutes = 15;
}
