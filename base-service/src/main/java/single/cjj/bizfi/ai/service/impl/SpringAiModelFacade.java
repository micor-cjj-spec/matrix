package single.cjj.bizfi.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;
import single.cjj.bizfi.ai.observability.AiRemoteMetrics;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Remote adapter for the independent Spring AI service.
 */
@Service
public class SpringAiModelFacade implements AiModelFacade {

    private static final String INTERNAL_TOKEN_HEADER = "X-Matrix-Internal-Token";

    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final AiServiceEndpointResolver endpointResolver;
    private final SpringAiCircuitBreaker circuitBreaker;
    private final AiRemoteMetrics metrics;
    private final HttpClient httpClient;

    @Autowired
    public SpringAiModelFacade(
            ObjectMapper objectMapper,
            AiProperties aiProperties,
            AiServiceEndpointResolver endpointResolver,
            SpringAiCircuitBreaker circuitBreaker,
            AiRemoteMetrics metrics
    ) {
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
        this.endpointResolver = endpointResolver;
        this.circuitBreaker = circuitBreaker;
        this.metrics = metrics;
        this.httpClient = buildHttpClient();
    }

    SpringAiModelFacade(
            ObjectMapper objectMapper,
            AiProperties aiProperties,
            AiServiceEndpointResolver endpointResolver,
            SpringAiCircuitBreaker circuitBreaker
    ) {
        this(objectMapper, aiProperties, endpointResolver, circuitBreaker, AiRemoteMetrics.isolated());
    }

    @Override
    public AiModelResult chat(AiModelRequest request) {
        validateRequest(request);
        ensureConfigured();
        long startedAt = metrics.start();
        acquirePermission("chat", startedAt);

        List<URI> candidates;
        try {
            candidates = endpointResolver.resolveCandidates();
        } catch (RuntimeException failure) {
            return failRequest("chat", startedAt, failure);
        }

        int attempts = Math.min(resolveMaxAttempts(), candidates.size());
        RuntimeException lastFailure = null;
        for (int i = 0; i < attempts; i++) {
            URI endpoint = candidates.get(i);
            metrics.recordAttempt("chat", endpoint);
            try {
                AiModelResult result = callChat(endpoint, request);
                circuitBreaker.recordSuccess();
                metrics.recordRequest("chat", "success", startedAt);
                return result;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                boolean retriable = isRetriable(failure);
                if (retriable && i + 1 < attempts) {
                    metrics.recordRetry("chat", failureReason(failure));
                }
                if (!retriable) {
                    break;
                }
            }
        }

        return failRequest("chat", startedAt, lastFailure);
    }

    @Override
    public AiModelResult stream(AiModelRequest request, Consumer<String> deltaConsumer) {
        validateRequest(request);
        if (deltaConsumer == null) {
            throw new IllegalArgumentException("deltaConsumer 不能为空");
        }
        ensureConfigured();
        long startedAt = metrics.start();
        acquirePermission("stream", startedAt);

        List<URI> candidates;
        try {
            candidates = endpointResolver.resolveCandidates();
        } catch (RuntimeException failure) {
            return failRequest("stream", startedAt, failure);
        }

        int attempts = Math.min(resolveMaxAttempts(), candidates.size());
        RuntimeException lastFailure = null;
        for (int i = 0; i < attempts; i++) {
            URI endpoint = candidates.get(i);
            AtomicBoolean emitted = new AtomicBoolean(false);
            metrics.recordAttempt("stream", endpoint);
            try {
                AiModelResult result = callStream(endpoint, request, delta -> {
                    emitted.set(true);
                    deltaConsumer.accept(delta);
                });
                circuitBreaker.recordSuccess();
                metrics.recordRequest("stream", "success", startedAt);
                return result;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                boolean retriable = !emitted.get() && isRetriable(failure);
                if (retriable && i + 1 < attempts) {
                    metrics.recordRetry("stream", failureReason(failure));
                }
                if (!retriable) {
                    break;
                }
            }
        }

        return failRequest("stream", startedAt, lastFailure);
    }

    @Override
    public AiConfigStatusResponse configStatus() {
        boolean endpointConfigured = Boolean.TRUE.equals(aiProperties.getSpringAiDiscoveryEnabled())
                || StringUtils.hasText(aiProperties.getSpringAiBaseUrl());
        boolean configured = endpointConfigured && StringUtils.hasText(aiProperties.getInternalToken());
        String model = Boolean.TRUE.equals(aiProperties.getSpringAiDiscoveryEnabled())
                ? "spring-ai-discovery"
                : "spring-ai-static";
        return new AiConfigStatusResponse(
                configured,
                configured ? model : "spring-ai-not-configured",
                configured ? "spring-ai" : "unavailable"
        );
    }

    private void acquirePermission(String operation, long startedAt) {
        try {
            circuitBreaker.acquirePermission();
        } catch (RuntimeException failure) {
            metrics.recordCircuitRejected(operation);
            metrics.recordRequest(operation, "circuit-open", startedAt);
            throw failure;
        }
    }

    private AiModelResult failRequest(String operation, long startedAt, RuntimeException failure) {
        if (circuitBreaker.recordFailure()) {
            metrics.recordCircuitOpened();
        }
        metrics.recordRequest(operation, "failure", startedAt);
        String prefix = "stream".equals(operation)
                ? "Spring AI 流式调用失败: "
                : "调用独立 Spring AI 服务失败: ";
        throw new IllegalStateException(prefix + safeMessage(failure), failure);
    }

    private AiModelResult callChat(URI endpoint, AiModelRequest request) {
        try {
            HttpRequest httpRequest = requestBuilder(endpoint, "/internal/model/chat")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response.statusCode(), response.body());
            return objectMapper.readValue(response.body(), AiModelResult.class);
        } catch (RemoteCallException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteCallException("调用被中断", e, false, "interrupted");
        } catch (Exception e) {
            throw new RemoteCallException(safeMessage(e), e, true, "transport");
        }
    }

    private AiModelResult callStream(
            URI endpoint,
            AiModelRequest request,
            Consumer<String> deltaConsumer
    ) {
        StringBuilder answer = new StringBuilder();
        AiModelResult[] completedResult = new AiModelResult[1];
        try {
            HttpRequest httpRequest = requestBuilder(endpoint, "/internal/model/chat/stream")
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

                    String payload = line.substring("data:".length());
                    if (payload.startsWith(" ")) {
                        payload = payload.substring(1);
                    }
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
                        case "error" -> throw new RemoteCallException(
                                data.path("message").asText("Spring AI 流式调用失败"),
                                null,
                                true,
                                "sse-error"
                        );
                        default -> {
                            // start/heartbeat events do not carry business data.
                        }
                    }
                }
            }

            if (completedResult[0] != null) {
                return completedResult[0];
            }
            if (!StringUtils.hasLength(answer)) {
                throw new RemoteCallException(
                        "Spring AI 流式响应未返回有效内容",
                        null,
                        true,
                        "empty-stream"
                );
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
        } catch (RemoteCallException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteCallException("调用被中断", e, false, "interrupted");
        } catch (Exception e) {
            throw new RemoteCallException(safeMessage(e), e, true, "transport");
        }
    }

    private HttpRequest.Builder requestBuilder(URI endpoint, String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(endpoint.toString() + path))
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
        if (!StringUtils.hasText(aiProperties.getInternalToken())) {
            throw new IllegalStateException("AI_INTERNAL_TOKEN 未配置");
        }
    }

    private void ensureSuccess(int statusCode, String body) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        String safeBody = body == null ? "" : body;
        if (safeBody.length() > 500) {
            safeBody = safeBody.substring(0, 500);
        }
        boolean retriable = statusCode == 408 || statusCode == 429 || statusCode >= 500;
        String reason = statusCode == 408
                ? "http-408"
                : statusCode == 429
                ? "http-429"
                : statusCode >= 500
                ? "http-5xx"
                : "http-4xx";
        throw new RemoteCallException("HTTP " + statusCode + ": " + safeBody, null, retriable, reason);
    }

    private boolean isRetriable(RuntimeException failure) {
        return failure instanceof RemoteCallException remote && remote.retriable;
    }

    private String failureReason(RuntimeException failure) {
        return failure instanceof RemoteCallException remote ? remote.reason : "unknown";
    }

    private int resolveMaxAttempts() {
        Integer configured = aiProperties.getSpringAiMaxAttempts();
        return configured != null && configured > 0 ? configured : 1;
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
                // Invalid proxy configuration must not prevent startup.
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

    private static final class RemoteCallException extends RuntimeException {
        private final boolean retriable;
        private final String reason;

        private RemoteCallException(String message, Throwable cause, boolean retriable, String reason) {
            super(message, cause);
            this.retriable = retriable;
            this.reason = reason;
        }
    }
}
