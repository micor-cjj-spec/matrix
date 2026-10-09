package single.cjj.bizfi.ai.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.bizfi.ai.config.AiProperties;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FinanceAiAuditClient {

    private static final String TOKEN_HEADER = "X-Matrix-AI-Audit-Token";
    private static final String OPERATOR_ID_HEADER = "X-Matrix-Audit-Operator-Id";
    private static final String OPERATOR_ROLES_HEADER = "X-Matrix-Audit-Operator-Roles";
    private static final String ACCESS_REQUEST_ID_HEADER = "X-Matrix-Audit-Request-Id";

    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final HttpClient httpClient;

    public FinanceAiAuditClient(ObjectMapper objectMapper, AiProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public AiToolExecutionAuditResponse execution(
            AiAuditOperatorContext operator,
            String requestId
    ) {
        String path = auditPath() + "/" + encode(requestId);
        return send(
                operator,
                HttpRequest.newBuilder().uri(URI.create(path)).GET(),
                AiToolExecutionAuditResponse.class
        );
    }

    public AiToolExecutionAuditPageResponse executions(
            AiAuditOperatorContext operator,
            Long userId,
            Long organizationId,
            String period,
            String status,
            String conversationId,
            String modelTraceId,
            String createdFrom,
            String createdTo,
            Integer page,
            Integer size
    ) {
        Map<String, Object> parameters = new java.util.LinkedHashMap<>();
        parameters.put("userId", userId);
        parameters.put("organizationId", organizationId);
        parameters.put("period", period);
        parameters.put("status", status);
        parameters.put("conversationId", conversationId);
        parameters.put("modelTraceId", modelTraceId);
        parameters.put("createdFrom", createdFrom);
        parameters.put("createdTo", createdTo);
        parameters.put("page", page);
        parameters.put("size", size);
        String path = auditPath() + queryString(parameters);
        return send(
                operator,
                HttpRequest.newBuilder().uri(URI.create(path)).GET(),
                AiToolExecutionAuditPageResponse.class
        );
    }

    public AiToolExecutionReconciliationResponse reconcileStale(AiAuditOperatorContext operator) {
        return send(
                operator,
                HttpRequest.newBuilder()
                        .uri(URI.create(auditPath() + "/reconcile-stale"))
                        .POST(HttpRequest.BodyPublishers.noBody()),
                AiToolExecutionReconciliationResponse.class
        );
    }

    private <T> T send(
            AiAuditOperatorContext operator,
            HttpRequest.Builder builder,
            Class<T> responseType
    ) {
        validateConfiguration();
        String accessRequestId = "audit_" + UUID.randomUUID().toString().replace("-", "");
        HttpRequest request = builder
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                .header("Accept", "application/json")
                .header(TOKEN_HEADER, properties.getFinanceAuditInternalToken())
                .header(OPERATOR_ID_HEADER, String.valueOf(operator.userId()))
                .header(OPERATOR_ROLES_HEADER, roleHeader(operator))
                .header(ACCESS_REQUEST_ID_HEADER, accessRequestId)
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw remoteFailure(response.statusCode(), response.body());
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "财务审计服务调用被中断",
                    failure
            );
        } catch (ResponseStatusException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "财务审计服务调用失败",
                    failure
            );
        }
    }

    private ResponseStatusException remoteFailure(int statusCode, String body) {
        HttpStatus status = switch (statusCode) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return new ResponseStatusException(
                status,
                "财务审计服务返回 HTTP " + statusCode + ": " + safeBody(body)
        );
    }

    private String auditPath() {
        return normalizeBaseUrl(properties.getFinanceAuditBaseUrl())
                + "/internal/ai/audit/tool-executions";
    }

    private String queryString(Map<String, Object> parameters) {
        List<String> values = new ArrayList<>();
        parameters.forEach((key, value) -> {
            if (value != null && StringUtils.hasText(value.toString())) {
                values.add(encode(key) + "=" + encode(value.toString().trim()));
            }
        });
        return values.isEmpty() ? "" : "?" + String.join("&", values);
    }

    private String roleHeader(AiAuditOperatorContext operator) {
        return operator.roles().stream()
                .sorted(Comparator.naturalOrder())
                .reduce((left, right) -> left + "," + right)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "审计操作员角色不能为空"
                ));
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getFinanceAuditBaseUrl())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "FINANCE_AI_AUDIT_BASE_URL 未配置"
            );
        }
        if (!StringUtils.hasText(properties.getFinanceAuditInternalToken())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "FINANCE_AI_AUDIT_INTERNAL_TOKEN 未配置"
            );
        }
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private long resolveTimeoutSeconds() {
        Integer configured = properties.getRequestTimeoutSeconds();
        return configured != null && configured > 0 ? configured : 60L;
    }

    private String safeBody(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String sanitized = value.replace('\r', ' ').replace('\n', ' ');
        return sanitized.length() > 500 ? sanitized.substring(0, 500) : sanitized;
    }
}
