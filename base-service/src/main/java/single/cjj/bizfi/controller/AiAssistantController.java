package single.cjj.bizfi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/ai")
public class AiAssistantController {

    private static final Map<String, Conversation> CONVERSATIONS = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = buildHttpClient();

    @Value("${AI_API_KEY:}")
    private String aiApiKey;

    @Value("${AI_BASE_URL:https://api.openai.com/v1}")
    private String aiBaseUrl;

    @Value("${AI_CHAT_MODEL:gemini-3-flash-preview}")
    private String aiChatModel;

    @PostMapping("/conversations")
    public ApiResponse<Map<String, Object>> createConversation(@RequestBody(required = false) CreateConversationRequest req) {
        String conversationId = "c_" + System.currentTimeMillis();
        String title = req != null && req.getTitle() != null && !req.getTitle().isBlank() ? req.getTitle() : "新会话";

        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setTitle(title);
        conversation.setScene(req != null ? req.getScene() : "knowledge_qa");
        conversation.setCreatedAt(LocalDateTime.now().toString());
        conversation.setMessages(new ArrayList<>());

        Message welcome = new Message();
        welcome.setRole("assistant");
        welcome.setContent("你好，我是 AI 助手。请告诉我你想解决的业务问题。");
        conversation.getMessages().add(welcome);

        CONVERSATIONS.put(conversationId, conversation);

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("title", title);
        return ApiResponse.success(data);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<Map<String, Object>> getMessages(@PathVariable String conversationId) {
        Conversation conversation = CONVERSATIONS.get(conversationId);
        if (conversation == null) {
            return ApiResponse.error(404, "会话不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("messages", conversation.getMessages());
        return ApiResponse.success(data);
    }

    @GetMapping("/config/status")
    public ApiResponse<Map<String, Object>> configStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("configured", isRealAiConfigured());
        data.put("baseUrl", aiBaseUrl);
        data.put("model", aiChatModel);
        data.put("mode", isRealAiConfigured() ? "real-model" : "fallback");
        data.put("keyPrefix", maskPrefix(aiApiKey));
        data.put("keySuffix", maskSuffix(aiApiKey));
        data.put("endpoint", buildEndpoint());
        return ApiResponse.success(data);
    }

    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(@RequestBody ChatRequest req) {
        if (req == null || req.getUserMessage() == null || req.getUserMessage().isBlank()) {
            return ApiResponse.error("userMessage 不能为空");
        }

        String conversationId = req.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "c_" + System.currentTimeMillis();
            Conversation autoConversation = new Conversation();
            autoConversation.setConversationId(conversationId);
            autoConversation.setTitle("快速提问");
            autoConversation.setScene("quick_chat");
            autoConversation.setCreatedAt(LocalDateTime.now().toString());
            autoConversation.setMessages(new ArrayList<>());
            CONVERSATIONS.put(conversationId, autoConversation);
        }

        Conversation conversation = CONVERSATIONS.get(conversationId);
        if (conversation == null) {
            return ApiResponse.error(404, "会话不存在");
        }

        Message userMsg = new Message();
        userMsg.setRole("user");
        userMsg.setContent(req.getUserMessage());
        conversation.getMessages().add(userMsg);

        String answer;
        String mode;
        try {
            if (isRealAiConfigured()) {
                answer = callRealModel(conversation, req.getUserMessage());
                mode = "real-model";
            } else {
                answer = buildFallbackAnswer(req.getUserMessage());
                mode = "fallback";
            }
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "unknown error" : e.getMessage();
            if (msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            answer = "AI 服务调用失败：" + msg;
            mode = "error-fallback";
        }

        Message assistantMsg = new Message();
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(answer);
        conversation.getMessages().add(assistantMsg);

        Map<String, Object> usage = new HashMap<>();
        usage.put("promptTokens", Math.max(20, req.getUserMessage().length() * 2));
        usage.put("completionTokens", Math.max(20, answer.length() * 2));
        usage.put("totalTokens", (Integer) usage.get("promptTokens") + (Integer) usage.get("completionTokens"));
        usage.put("estimatedCost", 0.0);

        Map<String, Object> citation = new HashMap<>();
        citation.put("docId", "demo_doc");
        citation.put("docName", "AI助手演示知识库");
        citation.put("chunkId", "demo_chunk_001");
        citation.put("snippet", "当前为演示实现：后续可接入真实 RAG 检索与引用。");

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("answer", answer);
        data.put("citations", Collections.singletonList(citation));
        data.put("usage", usage);
        data.put("traceId", "trace_" + System.currentTimeMillis());
        data.put("mode", mode);
        return ApiResponse.success(data);
    }

    @PostMapping("/feedback")
    public ApiResponse<Boolean> feedback(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(true);
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

    private String maskPrefix(String value) {
        if (value == null || value.isBlank()) return "";
        return value.substring(0, Math.min(8, value.length()));
    }

    private String maskSuffix(String value) {
        if (value == null || value.isBlank()) return "";
        return value.substring(Math.max(0, value.length() - 4));
    }

    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));

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

    private String callRealModel(Conversation conversation, String userMessage) throws Exception {
        if (isGeminiNativeBaseUrl()) {
            return callGeminiNativeModel(userMessage);
        }

        String endpoint = buildEndpoint();

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiChatModel);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", userMessage));
        body.put("messages", messages);

        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = extractAssistantContent(contentNode);
        if (content.isBlank()) {
            throw new RuntimeException("模型返回为空: " + response.body());
        }
        return content;
    }

    private String callGeminiNativeModel(String userMessage) throws Exception {
        String endpoint = buildEndpoint();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", userMessage);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(textPart));
        Map<String, Object> body = new HashMap<>();
        body.put("contents", Collections.singletonList(content));

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

    public static class CreateConversationRequest {
        private String title;
        private String scene;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getScene() { return scene; }
        public void setScene(String scene) { this.scene = scene; }
    }

    public static class ChatRequest {
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

    public static class Conversation {
        private String conversationId;
        private String title;
        private String scene;
        private String createdAt;
        private List<Message> messages;

        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getScene() { return scene; }
        public void setScene(String scene) { this.scene = scene; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
    }

    public static class Message {
        private String role;
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
