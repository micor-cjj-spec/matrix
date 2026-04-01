package single.cjj.bizfi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/ai")
public class AiAssistantStreamController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = buildHttpClient();
    private final ExecutorService aiStreamExecutor = Executors.newCachedThreadPool();

    @Value("${AI_API_KEY:}")
    private String aiApiKey;

    @Value("${AI_BASE_URL:https://api.openai.com/v1}")
    private String aiBaseUrl;

    @Value("${AI_CHAT_MODEL:gemini-3-flash-preview}")
    private String aiChatModel;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody StreamChatRequest req) {
        SseEmitter emitter = new SseEmitter(60000L);

        if (req == null || req.getUserMessage() == null || req.getUserMessage().isBlank()) {
            sendSseEvent(emitter, "error", Map.of("message", "userMessage 不能为空", "code", 400));
            emitter.complete();
            return emitter;
        }

        String conversationId = req.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            sendSseEvent(emitter, "error", Map.of("message", "conversationId 不能为空", "code", 400));
            emitter.complete();
            return emitter;
        }

        AiAssistantController.Conversation conversation = getConversation(conversationId);
        if (conversation == null) {
            sendSseEvent(emitter, "error", Map.of("message", "会话不存在", "code", 404));
            emitter.complete();
            return emitter;
        }

        AiAssistantController.Message userMsg = new AiAssistantController.Message();
        userMsg.setRole("user");
        userMsg.setContent(req.getUserMessage());
        conversation.getMessages().add(userMsg);

        aiStreamExecutor.execute(() -> {
            StringBuilder answerBuilder = new StringBuilder();
            String mode = isRealAiConfigured() ? "real-model" : "fallback";
            try {
                sendSseEvent(emitter, "start", Map.of(
                        "conversationId", conversationId,
                        "mode", mode,
                        "model", aiChatModel
                ));

                if (!isRealAiConfigured()) {
                    String answer = buildFallbackAnswer(req.getUserMessage());
                    answerBuilder.append(answer);
                    sendSseEvent(emitter, "delta", Map.of("delta", answer));
                } else if (isGeminiNativeBaseUrl()) {
                    String answer = callGeminiNativeModel(conversation, req.getUserMessage());
                    answerBuilder.append(answer);
                    sendSseEvent(emitter, "delta", Map.of("delta", answer));
                } else {
                    streamOpenAiCompatible(conversation, req.getUserMessage(), emitter, answerBuilder);
                }

                AiAssistantController.Message assistantMsg = new AiAssistantController.Message();
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(answerBuilder.toString());
                conversation.getMessages().add(assistantMsg);

                sendSseEvent(emitter, "done", Map.of(
                        "conversationId", conversationId,
                        "answer", answerBuilder.toString(),
                        "mode", mode,
                        "model", aiChatModel,
                        "traceId", "trace_" + System.currentTimeMillis()
                ));
                emitter.complete();
            } catch (Exception e) {
                sendSseEvent(emitter, "error", Map.of(
                        "message", safeErrorMessage(e),
                        "code", 500
                ));
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @SuppressWarnings("unchecked")
    private AiAssistantController.Conversation getConversation(String conversationId) {
        try {
            Field field = AiAssistantController.class.getDeclaredField("CONVERSATIONS");
            field.setAccessible(true);
            Map<String, AiAssistantController.Conversation> conversations =
                    (ConcurrentHashMap<String, AiAssistantController.Conversation>) field.get(null);
            return conversations.get(conversationId);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isRealAiConfigured() {
        return aiApiKey != null && !aiApiKey.isBlank();
    }

    private boolean isGeminiNativeBaseUrl() {
        return aiBaseUrl != null
                && aiBaseUrl.contains("generativelanguage.googleapis.com")
                && !aiBaseUrl.contains("/openai");
    }

    private String buildEndpoint() {
        if (isGeminiNativeBaseUrl()) {
            return aiBaseUrl.endsWith("/") ? aiBaseUrl + "models/" + aiChatModel + ":generateContent?key=" + aiApiKey
                    : aiBaseUrl + "/models/" + aiChatModel + ":generateContent?key=" + aiApiKey;
        }
        return aiBaseUrl.endsWith("/") ? aiBaseUrl + "chat/completions" : aiBaseUrl + "/chat/completions";
    }

    private String safeErrorMessage(Throwable throwable) {
        String msg = throwable == null ? null : throwable.getMessage();
        if ((msg == null || msg.isBlank()) && throwable != null && throwable.getCause() != null) {
            msg = throwable.getCause().getMessage();
        }
        if ((msg == null || msg.isBlank()) && throwable != null && throwable.getClass() != null) {
            msg = throwable.getClass().getName();
        }
        if (msg == null || msg.isBlank()) {
            msg = "AI 服务暂不可用，请稍后重试。";
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception ignored) {
        }
    }

    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
        String httpsProxy = System.getenv("HTTPS_PROXY");
        String httpProxy = System.getenv("HTTP_PROXY");
        String proxy = (httpsProxy != null && !httpsProxy.isBlank()) ? httpsProxy : httpProxy;
        if (proxy != null && !proxy.isBlank()) {
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

    private void streamOpenAiCompatible(AiAssistantController.Conversation conversation,
                                        String userMessage,
                                        SseEmitter emitter,
                                        StringBuilder answerBuilder) throws Exception {
        String endpoint = buildEndpoint();

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiChatModel);
        body.put("stream", true);

        List<Map<String, String>> messages = new ArrayList<>();
        for (AiAssistantController.Message item : conversation.getMessages()) {
            if (item == null || item.getRole() == null || item.getContent() == null || item.getContent().isBlank()) {
                continue;
            }
            messages.add(Map.of("role", item.getRole(), "content", item.getContent()));
        }
        if (messages.isEmpty()) {
            messages.add(Map.of("role", "user", "content", userMessage));
        }
        body.put("messages", messages);

        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + errorBody);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
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
                String delta = extractAssistantContent(choice.path("delta").path("content"));
                if (delta == null || delta.isBlank()) {
                    delta = extractAssistantContent(choice.path("message").path("content"));
                }
                if (delta != null && !delta.isBlank()) {
                    answerBuilder.append(delta);
                    sendSseEvent(emitter, "delta", Collections.singletonMap("delta", delta));
                }
            }
        }
    }

    private String callGeminiNativeModel(AiAssistantController.Conversation conversation, String userMessage) throws Exception {
        String endpoint = buildEndpoint();
        List<Map<String, Object>> contents = new ArrayList<>();
        for (AiAssistantController.Message item : conversation.getMessages()) {
            if (item == null || item.getRole() == null || item.getContent() == null || item.getContent().isBlank()) {
                continue;
            }
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", item.getContent());
            Map<String, Object> content = new HashMap<>();
            content.put("role", "assistant".equals(item.getRole()) ? "model" : "user");
            content.put("parts", Collections.singletonList(textPart));
            contents.add(content);
        }
        if (contents.isEmpty()) {
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", userMessage);
            Map<String, Object> content = new HashMap<>();
            content.put("role", "user");
            content.put("parts", Collections.singletonList(textPart));
            contents.add(content);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        String contentText = textNode.asText("").trim();
        if (contentText.isBlank()) {
            throw new RuntimeException("模型返回为空: " + response.body());
        }
        return contentText;
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
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(textNode.asText(""));
                }
            }
            return sb.toString().trim();
        }
        return contentNode.asText("").trim();
    }

    private String buildFallbackAnswer(String userMessage) {
        if (userMessage.contains("应付") || userMessage.contains("报销")) {
            return "建议先按‘单据状态 + 时间范围 + 组织’三维筛查，再定位异常明细。你也可以在完整版里继续追问，我会按步骤拆解。";
        }
        if (userMessage.contains("总账") || userMessage.contains("凭证")) {
            return "先核对期间、账簿与科目范围，再做余额与发生额勾稽。若有差异，优先检查凭证来源与过账状态。";
        }
        return "当前未配置 AI_API_KEY，正在使用本地占位回复。配置后将自动切换到真实大模型。";
    }

    public static class StreamChatRequest {
        private String conversationId;
        private String userMessage;
        private List<String> kbIds;
        private Boolean stream;

        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
        public List<String> getKbIds() { return kbIds; }
        public void setKbIds(List<String> kbIds) { this.kbIds = kbIds; }
        public Boolean getStream() { return stream; }
        public void setStream(Boolean stream) { this.stream = stream; }
    }
}
