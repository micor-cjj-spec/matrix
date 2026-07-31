package single.cjj.bizfi.ai.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiChatRequest;
import single.cjj.bizfi.ai.dto.AiToolContext;
import single.cjj.bizfi.ai.service.AiToolPolicyService;
import single.cjj.bizfi.exception.BizException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DefaultAiToolPolicyService implements AiToolPolicyService {

    public static final String MONTH_END_CLOSE_CHECK = "month-end-close-check";

    private final AiProperties properties;

    public DefaultAiToolPolicyService(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiToolContext prepareContext(Long userId, AiChatRequest request) {
        if (request == null) {
            return null;
        }

        boolean toolCalling = isToolCallingTask(request.getTaskType());
        boolean hasToolFields = StringUtils.hasText(request.getToolName())
                || request.getOrganizationId() != null
                || StringUtils.hasText(request.getAccountingPeriod());
        if (!toolCalling) {
            if (hasToolFields) {
                throw new BizException("工具参数只能在 taskType=tool-calling 时使用");
            }
            return null;
        }

        if (!Boolean.TRUE.equals(properties.getToolCallingEnabled())) {
            throw new BizException("AI 工具调用当前未启用");
        }

        String toolName = normalizeToolName(request.getToolName());
        if (!MONTH_END_CLOSE_CHECK.equals(toolName)) {
            throw new BizException("当前仅支持只读月结检查工具: " + MONTH_END_CLOSE_CHECK);
        }

        Long organizationId = request.getOrganizationId();
        if (organizationId == null || organizationId <= 0) {
            throw new BizException("organizationId 必须为正数");
        }

        String period = normalizePeriod(request.getAccountingPeriod());
        authorizeOrganization(organizationId);

        return new AiToolContext(
                MONTH_END_CLOSE_CHECK,
                userId,
                organizationId,
                period,
                "tool_" + UUID.randomUUID().toString().replace("-", "")
        );
    }

    private void authorizeOrganization(Long organizationId) {
        if (Boolean.TRUE.equals(properties.getToolAllowAllOrganizations())) {
            return;
        }
        if (configuredOrganizations().contains(organizationId)) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            Set<String> expected = Set.of(
                    "org:" + organizationId,
                    "org_" + organizationId,
                    "organization:" + organizationId,
                    "organization_" + organizationId
            );
            boolean authorized = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(StringUtils::hasText)
                    .map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .anyMatch(expected::contains);
            if (authorized) {
                return;
            }
        }

        throw new BizException("当前用户无权通过 AI 查询组织 " + organizationId + " 的月结数据");
    }

    private Set<Long> configuredOrganizations() {
        String configured = properties.getToolAllowedOrganizationIds();
        if (!StringUtils.hasText(configured)) {
            return Set.of();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::parseLongQuietly)
                .filter(value -> value != null && value > 0)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Long parseLongQuietly(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizePeriod(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("accountingPeriod 不能为空，格式为 yyyy-MM");
        }
        String period = value.trim();
        try {
            return YearMonth.parse(period).toString();
        } catch (DateTimeParseException exception) {
            throw new BizException("accountingPeriod 格式错误，应为 yyyy-MM");
        }
    }

    private boolean isToolCallingTask(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return Set.of("tool", "tools", "tool-calling", "agent").contains(normalized);
    }

    private String normalizeToolName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
