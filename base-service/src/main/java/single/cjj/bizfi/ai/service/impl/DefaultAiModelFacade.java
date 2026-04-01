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

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultAiModelFacade implements AiModelFacade {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = buildHttpClient();
    private final AiProperties aiProperties;

    @Value("${AI_API_KEY:}")
    private String aiApiKey;

    @Value("${AI_BASE_URL:https://api.openai.com/v1}")
    private String aiBaseUrl;

    @Value("${AI_CHAT_MODEL:gemini-3-flash-preview}")
    private String aiChatModel;

    public DefaultAiModelFacade(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public AiModelResult chat(AiModelRequest request) {
        if (request == null || !StringUtils.hasText(request.getUserMessage())) {
            throw new IllegalArgumentException("userMessage 不能为空");
        }
        try {
            if (isRealAiConfigured()) {
                return callRealModel(request);
            }
            return buildFallbackResult(request.getUserMessage(), "fallback");
        } catch (Exception e) {
            String msg = e.getMessage();
            if (!StringUtils.hasText(msg) && e.getCause() != null) {
                msg = e.getCause().getMessage();
            }
            if (!StringUtils.hasText(msg)) {
                msg = e.getClass().getName();
            }
            if (msg != null && msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            return buildFallbackResult("AI 服务调用失败: " + msg, "error-fallback");
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
        String endpoint = buildEndpoint();
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiChatModel);

        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getHistoryMessages() != null) {
            for (AiMessageResponse item : request.getHistoryMessages()) {
                if (item == null || !StringUtils.hasText(item.getRole()) || !StringUtils.hasText(item.getContent())) {
                    continue;
                }
                messages.add(Map.of("role", item.getRole(), "content", item.getContent()));
            }
        }
        if (request.getKnowledgeSnippets() != null && !request.getKnowledgeSnippets().isEmpty()) {
            StringBuilder knowledgeContext = new StringBuilder("以下是业务知识参考，请优先据此回答：\n");
            for (String snippet : request.getKnowledgeSnippets()) {
                if (StringUtils.hasText(snippet)) {
                    knowledgeContext.append("- ").append(snippet.trim()).append("\n");
                }
            }
            messages.add(Map.of("role", "system", "content", knowledgeContext.toString().trim()));
        }
        messages.add(Map.of("role", "user", "content", request.getUserMessage().trim()));
        body.put("messages", messages);

        String json = objectMapper.writeValueAsString(body);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(resolveRequestTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = extractAssistantContent(contentNode);
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("模型返回为空: " + response.body());
        }
        return new AiModelResult(content, aiChatModel, "real-model",
                "trace_" + System.currentTimeMillis(), 0, 0, 0, 0.0);
    }

    private AiModelResult callGeminiNativeModel(AiModelRequest request) throws Exception {
        String endpoint = buildEndpoint();
        List<Map<String, Object>> contents = new ArrayList<>();
        if (request.getHistoryMessages() != null) {
            for (AiMessageResponse item : request.getHistoryMessages()) {
                if (item == null || !StringUtils.hasText(item.getRole()) || !StringUtils.hasText(item.getContent())) {
                    continue;
                }
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("text", item.getContent());
                Map<String, Object> content = new HashMap<>();
                content.put("role", "assistant".equals(item.getRole()) ? "model" : "user");
                content.put("parts", Collections.singletonList(textPart));
                contents.add(content);
            }
        }
        if (request.getKnowledgeSnippets() != null && !request.getKnowledgeSnippets().isEmpty()) {
            StringBuilder knowledgeContext = new StringBuilder("以下是业务知识参考，请优先据此回答：\n");
            for (String snippet : request.getKnowledgeSnippets()) {
                if (StringUtils.hasText(snippet)) {
                    knowledgeContext.append("- ").append(snippet.trim()).append("\n");
                }
            }
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", knowledgeContext.toString().trim());
            Map<String, Object> content = new HashMap<>();
            content.put("role", "user");
            content.put("parts", Collections.singletonList(textPart));
            contents.add(content);
        }
        Map<String, Object> userText = new HashMap<>();
        userText.put("text", request.getUserMessage().trim());
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("role", "user");
        userContent.put("parts", Collections.singletonList(userText));
        contents.add(userContent);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        String json = objectMapper.writeValueAsString(body);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(resolveRequestTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        String content = textNode.asText("").trim();
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("模型返回为空: " + response.body());
        }
        return new AiModelResult(content, aiChatModel, "real-model",
                "trace_" + System.currentTimeMillis(), 0, 0, 0, 0.0);
    }

    private String extractAssistantContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("").trim();
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
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(textNode.asText(""));
                }
            }
            return sb.toString().trim();
        }
        return contentNode.asText("").trim();
    }

    private AiModelResult buildFallbackResult(String text, String mode) {
        String answer = text;
        if ("fallback".equals(mode)) {
            answer = buildFallbackAnswer(text);
        }
        return new AiModelResult(answer, aiChatModel, mode,
                "trace_" + System.currentTimeMillis(), 0, 0, 0, 0.0);
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

    private long resolveRequestTimeoutSeconds() {
        Integer configured = aiProperties.getRequestTimeoutSeconds();
        return configured != null && configured > 0 ? configured : 60L;
    }
}
