package single.cjj.bizfi.ai.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeEvaluationSecurityValidatorTest {

    @Test
    void shouldIgnoreValidationWhileEvaluationIsDisabled() {
        KnowledgeEvaluationProperties evaluation = new KnowledgeEvaluationProperties();
        evaluation.setEnabled(false);
        KnowledgeAclProperties acl = new KnowledgeAclProperties();
        acl.setEnabled(true);
        acl.setAdminAuthorities("ROLE_ADMIN");

        assertDoesNotThrow(() -> new KnowledgeEvaluationSecurityValidator(evaluation, acl).validate());
    }

    @Test
    void shouldRequireWorkerAuthorityWhenAclAndEvaluationAreEnabled() {
        KnowledgeEvaluationProperties evaluation = new KnowledgeEvaluationProperties();
        evaluation.setEnabled(true);
        KnowledgeAclProperties acl = new KnowledgeAclProperties();
        acl.setEnabled(true);
        acl.setAdminAuthorities("ROLE_ADMIN");

        assertThrows(
                IllegalStateException.class,
                () -> new KnowledgeEvaluationSecurityValidator(evaluation, acl).validate()
        );
    }

    @Test
    void shouldAcceptWorkerAuthorityCaseInsensitively() {
        KnowledgeEvaluationProperties evaluation = new KnowledgeEvaluationProperties();
        evaluation.setEnabled(true);
        KnowledgeAclProperties acl = new KnowledgeAclProperties();
        acl.setEnabled(true);
        acl.setAdminAuthorities("ROLE_ADMIN,role_super_admin");

        assertDoesNotThrow(() -> new KnowledgeEvaluationSecurityValidator(evaluation, acl).validate());
    }
}
