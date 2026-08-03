package single.cjj.bizfi.ai.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Component
public class KnowledgeEvaluationSecurityValidator {

    public static final String WORKER_AUTHORITY = "ROLE_SUPER_ADMIN";

    private final KnowledgeEvaluationProperties evaluationProperties;
    private final KnowledgeAclProperties aclProperties;

    public KnowledgeEvaluationSecurityValidator(
            KnowledgeEvaluationProperties evaluationProperties,
            KnowledgeAclProperties aclProperties
    ) {
        this.evaluationProperties = evaluationProperties;
        this.aclProperties = aclProperties;
    }

    @PostConstruct
    public void validate() {
        if (!Boolean.TRUE.equals(evaluationProperties.getEnabled())
                || !Boolean.TRUE.equals(aclProperties.getEnabled())) {
            return;
        }
        boolean workerAllowed = Arrays.stream(safe(aclProperties.getAdminAuthorities()).split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .anyMatch(WORKER_AUTHORITY::equalsIgnoreCase);
        if (!workerAllowed) {
            throw new IllegalStateException(
                    "RAG evaluation requires " + WORKER_AUTHORITY
                            + " in AI_KNOWLEDGE_ACL_ADMIN_AUTHORITIES while ACL is enabled"
            );
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
