package single.cjj.fi.ai.tool;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "matrix.ai-tool")
public class FinanceAiToolProperties {
    private String internalToken;
    private Integer maxCheckItems = 20;
    private Integer maxWarnings = 20;
}
