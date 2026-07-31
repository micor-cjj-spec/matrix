package single.cjj.bizfi.ai.service;

/**
 * Authorization boundary for AI tools that access organization-scoped business data.
 */
public interface AiOrganizationPermissionService {

    void assertCanAccess(Long userId, Long organizationId);
}
