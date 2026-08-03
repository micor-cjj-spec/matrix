package single.cjj.bizfi.ai.config;

import org.springframework.context.annotation.Configuration;

/**
 * Knowledge retrieval routing marker.
 *
 * <p>The primary {@code AiKnowledgeService} is now provided by
 * {@code HybridAiKnowledgeService}. The management service remains available
 * under the explicit bean name {@code aiKnowledgeManagementService} for CRUD
 * and keyword fallback.</p>
 */
@Configuration
public class AiKnowledgeServiceRoutingConfig {
}
