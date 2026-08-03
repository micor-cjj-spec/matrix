package single.cjj.bizfi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bizfi.ai.knowledge-acl")
public class KnowledgeAclProperties {

    /**
     * Keep disabled until the V6 migration is applied and existing knowledge bases have ACL rows.
     */
    private Boolean enabled = false;

    /**
     * Emergency/system-administrator bypass. Comma-separated numeric user IDs.
     */
    private String adminUserIds = "";

    /**
     * Authorities that bypass per-knowledge-base ACL checks.
     */
    private String adminAuthorities = "ROLE_ADMIN,ROLE_SUPER_ADMIN,admin,super_admin";
}
