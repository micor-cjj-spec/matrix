package single.cjj.matrix.ai.tool.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class FinanceToolClient {

    public static final String TOKEN_HEADER = "X-Matrix-AI-Tool-Token";

    private final ObjectMapper objectMapper;
    private final MatrixAiProperties properties;
    private final HttpClient httpClient;

    public FinanceToolClient(ObjectMapper objectMapper, MatrixAiProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public FinanceMonthEndCloseResult monthEndCloseCheck(ModelContracts.ToolContext context) {
        validate(context);
        try {
            InternalRequest body = new InternalRequest(
                    context.requestedByUserId(),
                    context.organizationId(),
                    context.period(),
                    context.requestId(),
                    context.conversationId(),
                    context.modelName(),
                    context.modelTraceId()
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getFinanceToolBaseUrl())
                            + "/internal/ai/tools/month-end-close-check"))
                    .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header(TOKEN_HEADER, properties.getFinanceToolInternalToken())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("finance tool HTTP " + response.statusCode()
                        + ": " + safeBody(response.body()));
            }
            FinanceMonthEndCloseResult result = objectMapper.readValue(
                    response.body(),
                    FinanceMonthEndCloseResult.class
            );
            if (result == null || !Boolean.TRUE.equals(result.readOnly())) {
                throw new IllegalStateException("finance tool 返回了无效或非只读结果");
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("finance tool 调用被中断", exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("finance tool 调用失败: " + safeMessage(exception), exception);
        }
    }

    private void validate(ModelContracts.ToolContext context) {
        if (context == null) {
            throw new IllegalArgumentException("toolContext 不能为空");
        }
        if (!"month-end-close-check".equals(context.toolName())) {
            throw new IllegalArgumentException("不支持的 finance tool: " + context.toolName());
        }
        if (context.organizationId() == null || context.organizationId() <= 0) {
            throw new IllegalArgumentException("organizationId 无效");
        }
        if (!StringUtils.hasText(context.period())) {
            throw new IllegalArgumentException("period 不能为空");
        }
        if (!StringUtils.hasText(context.conversationId())
                || !StringUtils.hasText(context.modelName())
                || !StringUtils.hasText(context.modelTraceId())) {
            throw new IllegalArgumentException("tool correlation context 不完整");
        }
        if (!StringUtils.hasText(properties.getFinanceToolBaseUrl())) {
            throw new IllegalStateException("FINANCE_SERVICE_BASE_URL 未配置");
        }
        if (!StringUtils.hasText(properties.getFinanceToolInternalToken())) {
            throw new IllegalStateException("FINANCE_AI_TOOL_INTERNAL_TOKEN 未配置");
        }
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private long resolveTimeoutSeconds() {
        Integer configured = properties.getFinanceToolTimeoutSeconds();
        return configured != null && configured > 0 ? configured : 20L;
    }

    private String safeBody(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        if (!StringUtils.hasText(message)) {
            return throwable == null ? "unknown" : throwable.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private record InternalRequest(
            Long requestedByUserId,
            Long organizationId,
            String period,
            String requestId,
            String conversationId,
            String modelName,
            String modelTraceId
    ) {
    }
}
