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
    private static final int DEFAULT_MAX_LINES = 200;
    private static final int DEFAULT_DAILY_WRITE_QUOTA = 10000;

    private final ObjectMapper objectMapper;

    public OpenApiPermissionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VoucherPermission resolveVoucherPermission(OpenApiApp app, OpenApiGrant grant) {
        String tenantId = requireTenant(app);
        Set<String> statuses = new LinkedHashSet<>(VOUCHER_API_MAX_STATUSES);
        Set<String> organizationIds = new LinkedHashSet<>(WILDCARD_SCOPE);
        Set<String> bookIds = new LinkedHashSet<>(WILDCARD_SCOPE);
        int maxHistoryMonths = DEFAULT_HISTORY_MONTHS;

        if (grant != null && StringUtils.hasText(grant.getDataPermissionJson())) {
            JsonNode root = readPermission(grant);
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
        }

        if (statuses.isEmpty()) {
            throw new OpenApiCallException("OPENAPI_40303", "应用没有可查询的凭证状态", 403);
        }
        requireDataScope(organizationIds, bookIds, "查询");
        return new VoucherPermission(tenantId, statuses, organizationIds, bookIds, maxHistoryMonths);
    }

    public VoucherWritePermission resolveVoucherWritePermission(OpenApiApp app, OpenApiGrant grant) {
        String tenantId = requireTenant(app);
        Set<String> organizationIds = new LinkedHashSet<>(WILDCARD_SCOPE);
        Set<String> bookIds = new LinkedHashSet<>(WILDCARD_SCOPE);
        int maxLinesPerVoucher = DEFAULT_MAX_LINES;
        int dailyWriteQuota = DEFAULT_DAILY_WRITE_QUOTA;

        if (grant != null && StringUtils.hasText(grant.getDataPermissionJson())) {
            JsonNode root = readPermission(grant);
            organizationIds = parseScope(root.path("organizationIds"));
            bookIds = parseScope(root.path("bookIds"));
            maxLinesPerVoucher = Math.max(2, Math.min(
                    root.path("maxLinesPerVoucher").asInt(DEFAULT_MAX_LINES), 500
            ));
            dailyWriteQuota = Math.max(1, Math.min(
                    root.path("dailyWriteQuota").asInt(DEFAULT_DAILY_WRITE_QUOTA), 1_000_000
            ));
        }

        requireDataScope(organizationIds, bookIds, "写入");
        return new VoucherWritePermission(
                tenantId,
                organizationIds,
                bookIds,
                maxLinesPerVoucher,
                dailyWriteQuota
        );
    }

    private String requireTenant(OpenApiApp app) {
        if (app == null || !StringUtils.hasText(app.getTenantId())) {
            throw new OpenApiCallException("OPENAPI_50001", "应用租户配置缺失", 500);
        }
        return app.getTenantId().trim();
    }

    private JsonNode readPermission(OpenApiGrant grant) {
        try {
            return objectMapper.readTree(grant.getDataPermissionJson());
        } catch (Exception e) {
            throw new OpenApiCallException("OPENAPI_50001", "应用数据权限配置格式错误", 500);
        }
    }

    private void requireDataScope(Set<String> organizationIds, Set<String> bookIds, String operation) {
        if (organizationIds.isEmpty() || bookIds.isEmpty()) {
            throw new OpenApiCallException(
                    "OPENAPI_40303", "应用没有可" + operation + "的组织或账簿范围", 403
            );
        }
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

    public record VoucherWritePermission(
            String tenantId,
            Set<String> organizationIds,
            Set<String> bookIds,
            int maxLinesPerVoucher,
            int dailyWriteQuota) {

        public boolean allowsOrganization(String organizationId) {
            return organizationIds.contains("*") || organizationIds.contains(organizationId);
        }

        public boolean allowsBook(String bookId) {
            return bookIds.contains("*") || bookIds.contains(bookId);
        }
    }
}
