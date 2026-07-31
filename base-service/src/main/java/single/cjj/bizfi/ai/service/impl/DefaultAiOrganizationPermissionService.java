package single.cjj.bizfi.ai.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.service.AiOrganizationPermissionService;
import single.cjj.bizfi.exception.BizException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DefaultAiOrganizationPermissionService implements AiOrganizationPermissionService {

    private final AiProperties properties;

    public DefaultAiOrganizationPermissionService(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void assertCanAccess(Long userId, Long organizationId) {
        if (userId == null || userId <= 0) {
            throw new BizException("用户ID无效");
        }
        if (organizationId == null || organizationId <= 0) {
            throw new BizException("organizationId 必须为正数");
        }
        if (Boolean.TRUE.equals(properties.getToolAllowAllOrganizations())) {
            return;
        }
        if (configuredUserOrganizationPairs().contains(userId + ":" + organizationId)) {
            return;
        }
        if (hasOrganizationAuthority(organizationId)) {
            return;
        }
        throw new BizException("当前用户无权通过 AI 查询组织 " + organizationId + " 的月结数据");
    }

    private boolean hasOrganizationAuthority(Long organizationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        Set<String> expected = Set.of(
                "org:" + organizationId,
                "org_" + organizationId,
                "organization:" + organizationId,
                "organization_" + organizationId
        );
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(expected::contains);
    }

    private Set<String> configuredUserOrganizationPairs() {
        String configured = properties.getToolAllowedUserOrganizationPairs();
        if (!StringUtils.hasText(configured)) {
            return Set.of();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(this::isValidUserOrganizationPair)
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isValidUserOrganizationPair(String value) {
        String[] parts = value.split(":", -1);
        if (parts.length != 2) {
            return false;
        }
        try {
            return Long.parseLong(parts[0].trim()) > 0 && Long.parseLong(parts[1].trim()) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
