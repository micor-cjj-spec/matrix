package single.cjj.openapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.openapi.entity.OpenApiApp;
import single.cjj.openapi.entity.OpenApiGrant;
import single.cjj.openapi.exception.OpenApiCallException;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class OpenApiPermissionService {

    private static final Set<String> VOUCHER_API_MAX_STATUSES = Set.of("POSTED");
    private static final Set<String> WILDCARD_SCOPE = Set.of("*");
    private static final int DEFAULT_HISTORY_MONTHS = 24;

    private final ObjectMapper objectMapper;

    public OpenApiPermissionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VoucherPermission resolveVoucherPermission(OpenApiApp app, OpenApiGrant grant) {
        if (app == null || !StringUtils.hasText(app.getTenantId())) {
            throw new OpenApiCallException("OPENAPI_50001", "应用租户配置缺失", 500);
        }

        Set<String> statuses = new LinkedHashSet<>(VOUCHER_API_MAX_STATUSES);
        Set<String> organizationIds = new LinkedHashSet<>(WILDCARD_SCOPE);
        Set<String> bookIds = new LinkedHashSet<>(WILDCARD_SCOPE);
        int maxHistoryMonths = DEFAULT_HISTORY_MONTHS;

        if (grant != null && StringUtils.hasText(grant.getDataPermissionJson())) {
            try {
                JsonNode root = objectMapper.readTree(grant.getDataPermissionJson());
                JsonNode statusNode = root.path("allowedStatuses");
                if (statusNode.isArray()) {
                    Set<String> requested = new LinkedHashSet<>();
                    statusNode.forEach(item -> {
                        if (item.isTextual() && StringUtils.hasText(item.asText())) {
                            requested.add(item.asText().trim().toUpperCase());
                        }
                    });
                    statuses.retainAll(requested);
                }
                organizationIds = parseScope(root.path("organizationIds"));
                bookIds = parseScope(root.path("bookIds"));
                int configuredMonths = root.path("maxHistoryMonths").asInt(DEFAULT_HISTORY_MONTHS);
                maxHistoryMonths = Math.max(1, Math.min(configuredMonths, 120));
            } catch (OpenApiCallException e) {
                throw e;
            } catch (Exception e) {
                throw new OpenApiCallException("OPENAPI_50001", "应用数据权限配置格式错误", 500);
            }
        }

        if (statuses.isEmpty()) {
            throw new OpenApiCallException("OPENAPI_40303", "应用没有可查询的凭证状态", 403);
        }
        if (organizationIds.isEmpty() || bookIds.isEmpty()) {
            throw new OpenApiCallException("OPENAPI_40303", "应用没有可查询的组织或账簿范围", 403);
        }
        return new VoucherPermission(
                app.getTenantId().trim(),
                statuses,
                organizationIds,
                bookIds,
                maxHistoryMonths
        );
    }

    private Set<String> parseScope(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            return new LinkedHashSet<>(WILDCARD_SCOPE);
        }
        Set<String> result = new LinkedHashSet<>();
        node.forEach(item -> {
            if (item.isTextual() && StringUtils.hasText(item.asText())) {
                result.add(item.asText().trim());
            }
        });
        if (result.contains("*")) {
            return new LinkedHashSet<>(WILDCARD_SCOPE);
        }
        return result;
    }

    public record VoucherPermission(
            String tenantId,
            Set<String> allowedStatuses,
            Set<String> organizationIds,
            Set<String> bookIds,
            int maxHistoryMonths) {

        public boolean allowsOrganization(String organizationId) {
            return organizationIds.contains("*") || organizationIds.contains(organizationId);
        }

        public boolean allowsBook(String bookId) {
            return bookIds.contains("*") || bookIds.contains(bookId);
        }
    }
}
