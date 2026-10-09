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

    /**
     * Dedicated token for audit query and reconciliation endpoints.
     */
    private String auditInternalToken;

    private Boolean auditReconciliationEnabled = true;
    private Integer auditStartedTimeoutMinutes = 15;
    private Long auditReconciliationDelayMs = 300000L;
    private Integer auditReconciliationBatchSize = 100;
}
