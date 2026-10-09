package single.cjj.bizfi.ai.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiChatRequest;
import single.cjj.bizfi.ai.dto.AiToolContext;
import single.cjj.bizfi.ai.service.AiOrganizationPermissionService;
import single.cjj.bizfi.ai.service.AiToolPolicyService;
import single.cjj.bizfi.exception.BizException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DefaultAiToolPolicyService implements AiToolPolicyService {

    public static final String MONTH_END_CLOSE_CHECK = "month-end-close-check";

    private final AiProperties properties;
    private final AiOrganizationPermissionService organizationPermissionService;

    public DefaultAiToolPolicyService(
            AiProperties properties,
            AiOrganizationPermissionService organizationPermissionService
    ) {
        this.properties = properties;
        this.organizationPermissionService = organizationPermissionService;
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
        if (!RoutingAiModelFacade.ADAPTER_SPRING_AI.equals(normalizeAdapter(properties.getModelAdapter()))) {
            throw new BizException("AI 工具调用要求 AI_MODEL_ADAPTER=spring-ai");
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
        organizationPermissionService.assertCanAccess(userId, organizationId);

        return new AiToolContext(
                MONTH_END_CLOSE_CHECK,
                userId,
                organizationId,
                period,
                "tool_" + UUID.randomUUID().toString().replace("-", "")
        );
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

    private String normalizeAdapter(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }
}
