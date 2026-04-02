package single.cjj.bizfi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiMessageResponse;
import single.cjj.bizfi.ai.entity.BizfiAiConversation;
import single.cjj.bizfi.ai.entity.BizfiAiMessage;
import single.cjj.bizfi.ai.service.AiConversationService;
import single.cjj.bizfi.ai.service.AiCurrentUserService;
import single.cjj.bizfi.ai.service.AiKnowledgeService;
import single.cjj.bizfi.ai.service.AiPromptDocService;
import single.cjj.bizfi.exception.BizException;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
public class AiAssistantStreamController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = buildHttpClient();
    private final ExecutorService aiStreamExecutor = Executors.newCachedThreadPool();

    @Autowired
    private AiCurrentUserService currentUserService;

    @Autowired
    private AiConversationService conversationService;

    @Autowired
    private AiKnowledgeService knowledgeService;

    @Autowired(required = false)
    private AiPromptDocService promptDocService;

    @Autowired
    private AiProperties aiProperties;

    @org.springframework.beans.factory.annotation.Value("${AI_API_KEY:}")
    private String aiApiKey;

    @org.springframework.beans.factory.annotation.Value("${AI_BASE_URL:https://api.openai.com/v1}")
    private String aiBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${AI_CHAT_MODEL:gemini-3-flash-preview}")
    private String aiChatModel;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody StreamChatRequest req) {
        SseEmitter emitter = new SseEmitter(resolveRequestTimeoutMillis());

        if (req == null || !StringUtils.hasText(req.getUserMessage())) {
            sendSseEvent(emitter, "error", Map.of("message", "userMessage 不能为空", "code", 400));
            emitter.complete();
            return emitter;
        }
        if (!StringUtils.hasText(req.getConversationId())) {
            sendSseEvent(emitter, "error", Map.of("message", "conversationId 不能为空", "code", 400));
            emitter.complete();
            return emitter;
        }

        final Long userId;
        final BizfiAiConversation conversation;
        try {
            userId = currentUserService.currentUserId();
            conversation = conversationService.getOwnedConversation(userId, req.getConversationId().trim());
        } catch (BizException ex) {
            sendSseEvent(emitter, "error", Map.of("message", ex.getMessage(), "code", 404));
            emitter.complete();
            return emitter;
        }

        final String conversationId = conversation.getFconversationid();
        final String userMessage = req.getUserMessage().trim();
        conversationService.saveUserMessage(conversationId, userMessage);

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
                    String answer = buildFallbackAnswer(userMessage);
                    answerBuilder.append(answer);
                    sendSseEvent(emitter, "delta", Map.of("delta", answer));
                } else if (isGeminiNativeBaseUrl()) {
                    String answer = callGeminiNativeModel(userId, conversationId, userMessage, req.getKbIds());
                    answerBuilder.append(answer);
                    sendSseEvent(emitter, "delta", Map.of("delta", answer));
                } else {
                    streamOpenAiCompatible(userId, conversationId, userMessage, req.getKbIds(), emitter, answerBuilder);
                }

                String traceId = "trace_" + System.currentTimeMillis();
                conversationService.saveAssistantMessage(
                        conversationId,
                        answerBuilder.toString(),
                        aiChatModel,
                        mode,
                        traceId,
                        0,
                        0,
                        0
                );

                sendSseEvent(emitter, "done", Map.of(
                        "conversationId", conversationId,
                        "answer", answerBuilder.toString(),
                        "mode", mode,
                        "model", aiChatModel,
                        "traceId", traceId
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

    private long resolveRequestTimeoutSeconds() {
        Integer configured = aiProperties.getRequestTimeoutSeconds();
        return configured != null && configured > 0 ? configured : 60L;
    }

    private long resolveRequestTimeoutMillis() {
        return resolveRequestTimeoutSeconds() * 1000L;
    }

    private String safeErrorMessage(Throwable throwable) {
        String msg = throwable == null ? null : throwable.getMessage();
        if (!StringUtils.hasText(msg) && throwable != null && throwable.getCause() != null) {
            msg = throwable.getCause().getMessage();
        }
        if (!StringUtils.hasText(msg) && throwable != null && throwable.getClass() != null) {
            msg = throwable.getClass().getName();
        }
        if (!StringUtils.hasText(msg)) {
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

    private void streamOpenAiCompatible(Long userId,
                                        String conversationId,
                                        String userMessage,
                                        List<String> kbIds,
                                        SseEmitter emitter,
                                        StringBuilder answerBuilder) throws Exception {
        String endpoint = buildEndpoint();
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiChatModel);
        body.put("stream", true);
        body.put("messages", buildOpenAiMessages(userId, conversationId, userMessage, kbIds));

        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(resolveRequestTimeoutSeconds()))
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
                if (!StringUtils.hasText(delta)) {
                    delta = extractAssistantContent(choice.path("message").path("content"));
                }
                if (StringUtils.hasText(delta)) {
                    answerBuilder.append(delta);
                    sendSseEvent(emitter, "delta", Collections.singletonMap("delta", delta));
                }
            }
        }
    }

    private String callGeminiNativeModel(Long userId,
                                         String conversationId,
                                         String userMessage,
                                         List<String> kbIds) throws Exception {
        String endpoint = buildEndpoint();
        List<Map<String, Object>> contents = buildGeminiContents(userId, conversationId, userMessage, kbIds);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(resolveRequestTimeoutSeconds()))
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
        if (!StringUtils.hasText(contentText)) {
            throw new RuntimeException("模型返回为空: " + response.body());
        }
        return contentText;
    }

    private List<Map<String, String>> buildOpenAiMessages(Long userId,
                                                          String conversationId,
                                                          String userMessage,
                                                          List<String> kbIds) {
        List<Map<String, String>> messages = new ArrayList<>();
        String systemPrompt = loadSystemPrompt();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (AiMessageResponse item : toHistoryMessages(userId, conversationId, userMessage)) {
            if (item == null || !StringUtils.hasText(item.getRole()) || !StringUtils.hasText(item.getContent())) {
                continue;
            }
            messages.add(Map.of("role", item.getRole(), "content", item.getContent()));
        }
        String knowledgeContext = buildKnowledgeContext(userMessage, kbIds);
        if (StringUtils.hasText(knowledgeContext)) {
            messages.add(Map.of("role", "system", "content", knowledgeContext));
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    private List<Map<String, Object>> buildGeminiContents(Long userId,
                                                          String conversationId,
                                                          String userMessage,
                                                          List<String> kbIds) {
        List<Map<String, Object>> contents = new ArrayList<>();
        String systemPrompt = loadSystemPrompt();
        if (StringUtils.hasText(systemPrompt)) {
            contents.add(buildGeminiContent("user", "系统约束：\n" + systemPrompt));
        }
        for (AiMessageResponse item : toHistoryMessages(userId, conversationId, userMessage)) {
            if (item == null || !StringUtils.hasText(item.getRole()) || !StringUtils.hasText(item.getContent())) {
                continue;
            }
            contents.add(buildGeminiContent("assistant".equals(item.getRole()) ? "model" : "user", item.getContent()));
        }
        String knowledgeContext = buildKnowledgeContext(userMessage, kbIds);
        if (StringUtils.hasText(knowledgeContext)) {
            contents.add(buildGeminiContent("user", knowledgeContext));
        }
        contents.add(buildGeminiContent("user", userMessage));
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

    private List<AiMessageResponse> toHistoryMessages(Long userId, String conversationId, String currentQuestion) {
        List<BizfiAiMessage> dbMessages = conversationService.listMessages(userId, conversationId);
        if (dbMessages == null || dbMessages.isEmpty()) {
            return List.of();
        }
        List<AiMessageResponse> history = dbMessages.stream()
                .map(item -> new AiMessageResponse(item.getFrole(), item.getFcontent()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (!history.isEmpty()) {
            AiMessageResponse last = history.get(history.size() - 1);
            if ("user".equals(last.getRole()) && currentQuestion.equals(last.getContent())) {
                history.remove(history.size() - 1);
            }
        }
        return history;
    }

    private String buildKnowledgeContext(String question, List<String> kbIds) {
        List<AiCitationResponse> citations = knowledgeService.retrieve(question, kbIds);
        List<String> snippets = citations.stream()
                .map(AiCitationResponse::getSnippet)
                .filter(StringUtils::hasText)
                .toList();
        if (snippets.isEmpty()) {
            return null;
        }
        StringBuilder knowledgeContext = new StringBuilder("以下是业务知识参考，请优先据此回答：\n");
        for (String snippet : snippets) {
            knowledgeContext.append("- ").append(snippet.trim()).append("\n");
        }
        return knowledgeContext.toString().trim();
    }

    private String loadSystemPrompt() {
        if (promptDocService == null) {
            return null;
        }
        try {
            String prompt = promptDocService.loadSystemPrompt();
            return StringUtils.hasText(prompt) ? prompt.trim() : null;
        } catch (Exception ignored) {
            return null;
        }
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

    private String buildFallbackAnswer(String userMessage) {
        if (userMessage.contains("应付") || userMessage.contains("报销")) {
            return "建议先按“单据状态 + 时间范围 + 组织”三个维度筛查，再定位异常明细。你也可以在完整页里继续追问，我会按步骤拆解。";
        }
        if (userMessage.contains("总账") || userMessage.contains("凭证")) {
            return "先核对期间、账簿与科目范围，再做余额与发生额勾稽。若有差异，优先检查凭证来源与过账状态。";
        }
        return "当前未配置 AI_API_KEY，正在使用本地占位回答。配置后将自动切换到真实大模型。";
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
