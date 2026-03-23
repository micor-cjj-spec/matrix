package single.cjj.bizfi.controller;

import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/ai")
public class AiAssistantController {

    private static final Map<String, Conversation> CONVERSATIONS = new ConcurrentHashMap<>();

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

        String answer = buildAnswer(req.getUserMessage());

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
        citation.put("snippet", "当前为占位实现：后续接入真实 RAG 检索与引用。");

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("answer", answer);
        data.put("citations", Collections.singletonList(citation));
        data.put("usage", usage);
        data.put("traceId", "trace_" + System.currentTimeMillis());
        return ApiResponse.success(data);
    }

    @PostMapping("/feedback")
    public ApiResponse<Boolean> feedback(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(true);
    }

    private String buildAnswer(String userMessage) {
        if (userMessage.contains("应付") || userMessage.contains("报销")) {
            return "建议先按‘单据状态 + 时间范围 + 组织’三维筛查，再定位异常明细。你也可以在完整版里继续追问，我会按步骤拆解。";
        }
        if (userMessage.contains("总账") || userMessage.contains("凭证")) {
            return "先核对期间、账簿与科目范围，再做余额与发生额勾稽。若有差异，优先检查凭证来源与过账状态。";
        }
        return "收到。当前已打通 AI 对话接口骨架，下一步可接入真实大模型与知识库检索，实现可引用来源的业务问答。";
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
