package single.cjj.bizfi.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;
import single.cjj.bizfi.ai.dto.AiMessageResponse;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;
import single.cjj.bizfi.ai.service.AiModelFacade;
import single.cjj.bizfi.ai.service.AiPromptDocService;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Service
public class PromptDrivenAiModelFacade implements AiModelFacade {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = buildHttpClient();
    private final AiPromptDocService promptDocService;
    private final AiProperties aiProperties;

    @Value("${AI_API_KEY:}")
    private String aiApiKey;

    @Value("${AI_BASE_URL:https://api.openai.com/v1}")
    private String aiBaseUrl;

    @Value("${AI_CHAT_MODEL:gemini-3-flash-preview}")
    private String aiChatModel;

    public PromptDrivenAiModelFacade(AiPromptDocService promptDocService, AiProperties aiProperties) {
        this.promptDocService = promptDocService;
        this.aiProperties = aiProperties;
    }

    @Override
    public AiModelResult chat(AiModelRequest request) {
        validateRequest(request);
        try {
            if (isRealAiConfigured()) {
                return callRealModel(request);
            }
            return buildFallbackResult(request.getUserMessage(), "fallback");
        } catch (Exception e) {
            return buildFallbackResult("AI 服务调用失败: " + safeErrorMessage(e), "error-fallback");
        }
    }

    @Override
    public AiModelResult stream(AiModelRequest request, Consumer<String> deltaConsumer) {
        validateRequest(request);
        Objects.requireNonNull(deltaConsumer, "deltaConsumer 不能为空");

        AtomicBoolean emitted = new AtomicBoolean(false);
        Consumer<String> trackedConsumer = delta -> {
            if (delta != null && !delta.isEmpty()) {
                emitted.set(true);
                deltaConsumer.accept(delta);
            }
        };

        try {
            if (!isRealAiConfigured()) {
                AiModelResult fallback = buildFallbackResult(request.getUserMessage(), "fallback");
                trackedConsumer.accept(fallback.getAnswer());
                return fallback;
            }
            if (isGeminiNativeBaseUrl()) {
                AiModelResult result = callGeminiNativeModel(request);
                trackedConsumer.accept(result.getAnswer());
                return result;
            }
            return streamOpenAiCompatible(request, trackedConsumer);
        } catch (Exception e) {
            if (emitted.get()) {
                throw new IllegalStateException("AI 流式响应中断: " + safeErrorMessage(e), e);
            }
            AiModelResult fallback = buildFallbackResult(
                    "AI 服务调用失败: " + safeErrorMessage(e),
                    "error-fallback"
            );
            trackedConsumer.accept(fallback.getAnswer());
            return fallback;
        }
    }

    @Override
    public AiConfigStatusResponse configStatus() {
        return new AiConfigStatusResponse(
                isRealAiConfigured(),
                aiChatModel,
                isRealAiConfigured() ? "real-model" : "fallback"
        );
    }

    private void validateRequest(AiModelRequest request) {
        if (request == null || !StringUtils.hasText(request.getUserMessage())) {
            throw new IllegalArgumentException("userMessage 不能为空");
        }
    }

    private boolean isRealAiConfigured() {
        return StringUtils.hasText(aiApiKey);
    }

    private boolean isGeminiNativeBaseUrl() {
        return aiBaseUrl != null
                && aiBaseUrl.contains("generativelanguage.googleapis.com")
                && !aiBaseUrl.contains("/openai");
    }

    private String buildEndpoint() {
        if (isGeminiNativeBaseUrl()) {
            return aiBaseUrl.endsWith("/")
                    ? aiBaseUrl + "models/" + aiChatModel + ":generateContent?key=" + aiApiKey
                    : aiBaseUrl + "/models/" + aiChatModel + ":generateContent?key=" + aiApiKey;
        }
        return aiBaseUrl.endsWith("/") ? aiBaseUrl + "chat/completions" : aiBaseUrl + "/chat/completions";
    }

    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
        String httpsProxy = System.getenv("HTTPS_PROXY");
        String httpProxy = System.getenv("HTTP_PROXY");
        String proxy = StringUtils.hasText(httpsProxy) ? httpsProxy : httpProxy;
        if (StringUtils.hasText(proxy)) {
            try {
                URI proxyUri = URI.create(proxy);
                String host = proxyUri.getHost();
                int port = proxyUri.getPort();
                if (host != null && port > 0) {
                    builder.proxy(ProxySelector.of(new InetSocketAddress(host, port)));
                }
            } catch (Exception ignored) {
            }
        }
        return builder.build();
    }

    private AiModelResult callRealModel(AiModelRequest request) throws Exception {
        if (isGeminiNativeBaseUrl()) {
            return callGeminiNativeModel(request);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiChatModel);
        body.put("messages", buildOpenAiMessages(request));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(buildEndpoint()))
                .timeout(Duration.ofSeconds(resolveRequestTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response.statusCode(), response.body());

        JsonNode root = objectMapper.readTree(response.body());
        String content = extractAssistantContent(root.path("choices").path(0).path("message").path("content"));
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("模型返回为空: " + response.body());
        }
        return buildRealResult(content);
    }

    private AiModelResult streamOpenAiCompatible(
            AiModelRequest request,
            Consumer<String> deltaConsumer
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiChatModel);
        body.put("stream", true);
        body.put("messages", buildOpenAiMessages(request));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(buildEndpoint()))
                .timeout(Duration.ofSeconds(resolveRequestTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<InputStream> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofInputStream()
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + errorBody);
        }

        StringBuilder answer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || !line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if ("[DONE]".equals(payload)) {
                    break;
                }

                JsonNode root = objectMapper.readTree(payload);
                JsonNode choice = root.path("choices").path(0);
                String delta = extractStreamingContent(choice.path("delta").path("content"));
                if (delta.isEmpty()) {
                    delta = extractStreamingContent(choice.path("message").path("content"));
                }
                if (!delta.isEmpty()) {
                    answer.append(delta);
                    deltaConsumer.accept(delta);
                }
            }
        }

        String finalAnswer = answer.toString().trim();
        if (!StringUtils.hasText(finalAnswer)) {
            throw new RuntimeException("模型流式返回为空");
        }
        return buildRealResult(finalAnswer);
    }

    private AiModelResult callGeminiNativeModel(AiModelRequest request) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contents", buildGeminiContents(request));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(buildEndpoint()))
                .timeout(Duration.ofSeconds(resolveRequestTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response.statusCode(), response.body());

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText("")
                .trim();
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("模型返回为空: " + response.body());
        }
        return buildRealResult(content);
    }

    private List<Map<String, String>> buildOpenAiMessages(AiModelRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        String systemPrompt = promptDocService.loadSystemPrompt();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt.trim()));
        }
        if (request.getHistoryMessages() != null) {
            for (AiMessageResponse item : request.getHistoryMessages()) {
                if (item == null || !StringUtils.hasText(item.getRole()) || !StringUtils.hasText(item.getContent())) {
                    continue;
                }
                messages.add(Map.of("role", item.getRole(), "content", item.getContent()));
            }
        }
        String knowledgeContext = buildKnowledgeContext(request.getKnowledgeSnippets());
        if (StringUtils.hasText(knowledgeContext)) {
            messages.add(Map.of("role", "system", "content", knowledgeContext));
        }
        messages.add(Map.of("role", "user", "content", request.getUserMessage().trim()));
        return messages;
    }

    private List<Map<String, Object>> buildGeminiContents(AiModelRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();
        String systemPrompt = promptDocService.loadSystemPrompt();
        if (StringUtils.hasText(systemPrompt)) {
            contents.add(buildGeminiContent("user", "系统约束：\n" + systemPrompt.trim()));
        }
        if (request.getHistoryMessages() != null) {
            for (AiMessageResponse item : request.getHistoryMessages()) {
                if (item == null || !StringUtils.hasText(item.getRole()) || !StringUtils.hasText(item.getContent())) {
                    continue;
                }
                String role = "assistant".equals(item.getRole()) ? "model" : "user";
                contents.add(buildGeminiContent(role, item.getContent()));
            }
        }
        String knowledgeContext = buildKnowledgeContext(request.getKnowledgeSnippets());
        if (StringUtils.hasText(knowledgeContext)) {
            contents.add(buildGeminiContent("user", knowledgeContext));
        }
        contents.add(buildGeminiContent("user", request.getUserMessage().trim()));
        return contents;
    }

    private Map<String, Object> buildGeminiContent(String role, String text) {
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", text);
        Map<String, Object> content = new HashMap<>();
        content.put("role", role);
        content.put("parts", Collections.singletonList(textPart));
        return content;
    }

    private String buildKnowledgeContext(List<String> snippets) {
        if (snippets == null || snippets.isEmpty()) {
            return null;
        }
        StringBuilder context = new StringBuilder("以下是业务知识参考，请优先据此回答：\n");
        for (String snippet : snippets) {
            if (StringUtils.hasText(snippet)) {
                context.append("- ").append(snippet.trim()).append("\n");
            }
        }
        return context.length() == 0 ? null : context.toString().trim();
    }

    private String extractAssistantContent(JsonNode contentNode) {
        return extractStreamingContent(contentNode).trim();
    }

    /**
     * 流式 delta 必须保留前导空格，否则逐段 trim 后会把英文单词粘连。
     */
    private String extractStreamingContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item == null || item.isNull()) {
                    continue;
                }
                if (item.isTextual()) {
                    sb.append(item.asText(""));
                    continue;
                }
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    sb.append(textNode.asText(""));
                }
            }
            return sb.toString();
        }
        return contentNode.asText("");
    }

    private void ensureSuccess(int statusCode, String body) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new RuntimeException("HTTP " + statusCode + ": " + body);
        }
    }

    private AiModelResult buildRealResult(String answer) {
        return new AiModelResult(
                answer,
                aiChatModel,
                "real-model",
                "trace_" + System.currentTimeMillis(),
                0,
                0,
                0,
                0.0
        );
    }

    private AiModelResult buildFallbackResult(String text, String mode) {
        String answer = "fallback".equals(mode) ? buildFallbackAnswer(text) : text;
        return new AiModelResult(
                answer,
                aiChatModel,
                mode,
                "trace_" + System.currentTimeMillis(),
                0,
                0,
                0,
                0.0
        );
    }

    private String buildFallbackAnswer(String userMessage) {
        if (userMessage.contains("应付") || userMessage.contains("报销")) {
            return "建议先按“单据状态 + 时间范围 + 组织”三个维度筛查，再定位异常明细。你也可以在完整页里继续追问，我会按步骤拆解。";
        }
        if (userMessage.contains("总账") || userMessage.contains("凭证")) {
            return "先核对期间、账簿与科目范围，再做余额与发生额勾稽。若有差异，优先检查凭证来源与过账状态。";
        }
        return "当前未配置 AI_API_KEY，正在使用本地占位回答。配置后将自动切换到真实大模型。";
    }

    private String safeErrorMessage(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        if (!StringUtils.hasText(message) && throwable != null && throwable.getCause() != null) {
            message = throwable.getCause().getMessage();
        }
        if (!StringUtils.hasText(message) && throwable != null) {
            message = throwable.getClass().getName();
        }
        if (!StringUtils.hasText(message)) {
            message = "AI 服务暂不可用，请稍后重试。";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private long resolveRequestTimeoutSeconds() {
        Integer configured = aiProperties.getRequestTimeoutSeconds();
        return configured != null && configured > 0 ? configured : 60L;
    }
}
