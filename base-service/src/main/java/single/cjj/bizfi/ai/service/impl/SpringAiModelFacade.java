package single.cjj.bizfi.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;
import single.cjj.bizfi.ai.service.AiModelFacade;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * 独立 ai-service 的 Spring AI 远程适配器。
 *
 * <p>base-service 继续负责会话、RAG 和消息持久化；模型生成通过内部
 * HTTP 协议交给采用 Spring Boot 3.5 / Spring AI 1.1 的 ai-service。</p>
 */
@Service
public class SpringAiModelFacade implements AiModelFacade {

    private static final String INTERNAL_TOKEN_HEADER = "X-Matrix-Internal-Token";

    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final HttpClient httpClient;

    public SpringAiModelFacade(ObjectMapper objectMapper, AiProperties aiProperties) {
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
        this.httpClient = buildHttpClient();
    }

    @Override
    public AiModelResult chat(AiModelRequest request) {
        validateRequest(request);
        ensureConfigured();
        try {
            HttpRequest httpRequest = requestBuilder("/internal/model/chat")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response.statusCode(), response.body());
            return objectMapper.readValue(response.body(), AiModelResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("调用独立 Spring AI 服务失败: " + safeMessage(e), e);
        }
    }

    @Override
    public AiModelResult stream(AiModelRequest request, Consumer<String> deltaConsumer) {
        validateRequest(request);
        if (deltaConsumer == null) {
            throw new IllegalArgumentException("deltaConsumer 不能为空");
        }
        ensureConfigured();

        StringBuilder answer = new StringBuilder();
        AiModelResult[] completedResult = new AiModelResult[1];
        try {
            HttpRequest httpRequest = requestBuilder("/internal/model/chat/stream")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                ensureSuccess(response.statusCode(), body);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8)
            )) {
                String eventName = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("event:")) {
                        eventName = line.substring("event:".length()).trim();
                        continue;
                    }
                    if (!line.startsWith("data:")) {
                        if (line.isBlank()) {
                            eventName = null;
                        }
                        continue;
                    }

                    String payload = line.substring("data:".length()).trim();
                    if (!StringUtils.hasText(payload)) {
                        continue;
                    }
                    JsonNode data = objectMapper.readTree(payload);
                    String type = data.path("type").asText(eventName == null ? "" : eventName);
                    switch (type) {
                        case "delta" -> {
                            String delta = data.path("delta").asText("");
                            if (StringUtils.hasLength(delta)) {
                                answer.append(delta);
                                deltaConsumer.accept(delta);
                            }
                        }
                        case "done" -> {
                            JsonNode resultNode = data.path("result");
                            if (!resultNode.isMissingNode() && !resultNode.isNull()) {
                                completedResult[0] = objectMapper.treeToValue(resultNode, AiModelResult.class);
                            }
                        }
                        case "error" -> throw new IllegalStateException(
                                data.path("message").asText("Spring AI 流式调用失败")
                        );
                        default -> {
                            // start/heartbeat 等事件无需业务处理。
                        }
                    }
                }
            }

            if (completedResult[0] != null) {
                return completedResult[0];
            }
            return new AiModelResult(
                    answer.toString(),
                    "spring-ai-remote",
                    "spring-ai",
                    "trace_" + System.currentTimeMillis(),
                    0,
                    0,
                    0,
                    0.0
            );
        } catch (Exception e) {
            throw new IllegalStateException("Spring AI 流式调用失败: " + safeMessage(e), e);
        }
    }

    @Override
    public AiConfigStatusResponse configStatus() {
        boolean configured = StringUtils.hasText(aiProperties.getSpringAiBaseUrl())
                && StringUtils.hasText(aiProperties.getInternalToken());
        return new AiConfigStatusResponse(
                configured,
                configured ? "spring-ai-remote" : "spring-ai-not-configured",
                configured ? "spring-ai" : "unavailable"
        );
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(aiProperties.getSpringAiBaseUrl()) + path))
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header(INTERNAL_TOKEN_HEADER, aiProperties.getInternalToken());
    }

    private void validateRequest(AiModelRequest request) {
        if (request == null || !StringUtils.hasText(request.getUserMessage())) {
            throw new IllegalArgumentException("userMessage 不能为空");
        }
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(aiProperties.getSpringAiBaseUrl())) {
            throw new IllegalStateException("AI_SERVICE_BASE_URL 未配置");
        }
        if (!StringUtils.hasText(aiProperties.getInternalToken())) {
            throw new IllegalStateException("AI_INTERNAL_TOKEN 未配置");
        }
    }

    private void ensureSuccess(int statusCode, String body) {
        if (statusCode < 200 || statusCode >= 300) {
            String safeBody = body == null ? "" : body;
            if (safeBody.length() > 500) {
                safeBody = safeBody.substring(0, 500);
            }
            throw new IllegalStateException("HTTP " + statusCode + ": " + safeBody);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private long resolveTimeoutSeconds() {
        Integer configured = aiProperties.getRequestTimeoutSeconds();
        return configured != null && configured > 0 ? configured : 60L;
    }

    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));
        String httpsProxy = System.getenv("HTTPS_PROXY");
        String httpProxy = System.getenv("HTTP_PROXY");
        String proxy = StringUtils.hasText(httpsProxy) ? httpsProxy : httpProxy;
        if (StringUtils.hasText(proxy)) {
            try {
                URI proxyUri = URI.create(proxy);
                if (proxyUri.getHost() != null && proxyUri.getPort() > 0) {
                    builder.proxy(ProxySelector.of(new InetSocketAddress(
                            proxyUri.getHost(),
                            proxyUri.getPort()
                    )));
                }
            } catch (Exception ignored) {
                // 非法代理配置不阻止服务启动。
            }
        }
        return builder.build();
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        if (!StringUtils.hasText(message) && throwable != null && throwable.getCause() != null) {
            message = throwable.getCause().getMessage();
        }
        if (!StringUtils.hasText(message)) {
            message = throwable == null ? "unknown" : throwable.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
