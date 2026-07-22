package single.cjj.openapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.openapi.entity.OpenApiGrant;
import single.cjj.openapi.exception.OpenApiCallException;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class OpenApiPermissionService {

    private static final Set<String> VOUCHER_API_MAX_STATUSES = Set.of("POSTED");
    private static final int DEFAULT_HISTORY_MONTHS = 24;

    private final ObjectMapper objectMapper;

    public OpenApiPermissionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VoucherPermission resolveVoucherPermission(OpenApiGrant grant) {
        Set<String> statuses = new LinkedHashSet<>(VOUCHER_API_MAX_STATUSES);
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
                int configuredMonths = root.path("maxHistoryMonths").asInt(DEFAULT_HISTORY_MONTHS);
                maxHistoryMonths = Math.max(1, Math.min(configuredMonths, 120));
            } catch (Exception e) {
                throw new OpenApiCallException("OPENAPI_50001", "应用数据权限配置格式错误", 500);
            }
        }

        if (statuses.isEmpty()) {
            throw new OpenApiCallException("OPENAPI_40303", "应用没有可查询的凭证状态", 403);
        }
        return new VoucherPermission(statuses, maxHistoryMonths);
    }

    public record VoucherPermission(Set<String> allowedStatuses, int maxHistoryMonths) {
    }
}
