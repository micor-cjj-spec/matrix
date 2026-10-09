package single.cjj.bizfi.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class AiEmbeddingClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Matrix-Internal-Token";

    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final AiServiceEndpointResolver endpointResolver;
    private final HttpClient httpClient;

    public AiEmbeddingClient(
            ObjectMapper objectMapper,
            AiProperties properties,
            AiServiceEndpointResolver endpointResolver
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.endpointResolver = endpointResolver;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public EmbeddingBatch embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("embedding texts 不能为空");
        }
        if (texts.size() > 32) {
            throw new IllegalArgumentException("单次 embedding 数量不能超过 32");
        }
        if (!StringUtils.hasText(properties.getInternalToken())) {
            throw new IllegalStateException("AI_INTERNAL_TOKEN 未配置");
        }

        List<URI> candidates = endpointResolver.resolveCandidates();
        int attempts = Math.min(resolveMaxAttempts(), candidates.size());
        RuntimeException lastFailure = null;
        for (int index = 0; index < attempts; index++) {
            URI endpoint = candidates.get(index);
            try {
                return call(endpoint, texts);
            } catch (EmbeddingRemoteException failure) {
                lastFailure = failure;
                if (!failure.retriable() || index + 1 >= attempts) {
                    break;
                }
            }
        }
        throw new IllegalStateException(
                "调用 AI Embedding 服务失败: " + safeMessage(lastFailure),
                lastFailure
        );
    }

    private EmbeddingBatch call(URI endpoint, List<String> texts) {
        try {
            String body = objectMapper.writeValueAsString(new EmbeddingRequest(texts));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint.toString() + "/internal/model/embeddings"))
                    .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header(INTERNAL_TOKEN_HEADER, properties.getInternalToken())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            ensureSuccess(response.statusCode(), response.body());
            EmbeddingBatch batch = objectMapper.readValue(response.body(), EmbeddingBatch.class);
            validateBatch(batch, texts.size());
            return batch;
        } catch (EmbeddingRemoteException failure) {
            throw failure;
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new EmbeddingRemoteException("调用被中断", failure, false);
        } catch (Exception failure) {
            throw new EmbeddingRemoteException(safeMessage(failure), failure, true);
        }
    }

    private void validateBatch(EmbeddingBatch batch, int expectedSize) {
        if (batch == null || batch.vectors() == null || batch.vectors().size() != expectedSize) {
            throw new EmbeddingRemoteException("Embedding 返回数量与请求不一致", null, false);
        }
        if (batch.dimensions() == null || batch.dimensions() <= 0) {
            throw new EmbeddingRemoteException("Embedding 返回的向量维度无效", null, false);
        }
        for (List<Double> vector : batch.vectors()) {
            if (vector == null || vector.size() != batch.dimensions()) {
                throw new EmbeddingRemoteException("Embedding 返回的向量维度不一致", null, false);
            }
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
        throw new EmbeddingRemoteException(
                "HTTP " + statusCode + ": " + safeBody,
                null,
                retriable
        );
    }

    private int resolveMaxAttempts() {
        Integer configured = properties.getSpringAiMaxAttempts();
        return configured != null && configured > 0 ? configured : 1;
    }

    private long resolveTimeoutSeconds() {
        Integer configured = properties.getRequestTimeoutSeconds();
        return configured != null && configured > 0 ? configured : 60L;
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        if (!StringUtils.hasText(message) && throwable != null && throwable.getCause() != null) {
            message = throwable.getCause().getMessage();
        }
        if (!StringUtils.hasText(message)) {
            return throwable == null ? "unknown" : throwable.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    public record EmbeddingBatch(
            String model,
            Integer dimensions,
            List<List<Double>> vectors
    ) {
    }

    private record EmbeddingRequest(List<String> texts) {
    }

    private static final class EmbeddingRemoteException extends RuntimeException {
        private final boolean retriable;

        private EmbeddingRemoteException(String message, Throwable cause, boolean retriable) {
            super(message, cause);
            this.retriable = retriable;
        }

        private boolean retriable() {
            return retriable;
        }
    }
}
