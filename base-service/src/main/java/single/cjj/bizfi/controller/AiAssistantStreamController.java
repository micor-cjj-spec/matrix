package single.cjj.bizfi.controller;

import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiChatRequest;
import single.cjj.bizfi.ai.dto.AiChatResponse;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;
import single.cjj.bizfi.ai.service.AiChatService;
import single.cjj.bizfi.ai.service.AiChatStreamObserver;
import single.cjj.bizfi.ai.service.AiCurrentUserService;
import single.cjj.bizfi.exception.BizException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/ai")
public class AiAssistantStreamController {

    private final AiCurrentUserService currentUserService;
    private final AiChatService aiChatService;
    private final AiProperties aiProperties;
    private final ExecutorService aiStreamExecutor = new DelegatingSecurityContextExecutorService(
            Executors.newCachedThreadPool()
    );

    public AiAssistantStreamController(
            AiCurrentUserService currentUserService,
            AiChatService aiChatService,
            AiProperties aiProperties
    ) {
        this.currentUserService = currentUserService;
        this.aiChatService = aiChatService;
        this.aiProperties = aiProperties;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody(required = false) AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(resolveRequestTimeoutMillis());

        if (request == null || !StringUtils.hasText(request.getUserMessage())) {
            sendError(emitter, "userMessage 不能为空", 400);
            return emitter;
        }

        final Long userId;
        try {
            userId = currentUserService.currentUserId();
        } catch (BizException ex) {
            sendError(emitter, safeErrorMessage(ex), 401);
            return emitter;
        }

        aiStreamExecutor.execute(() -> executeStream(userId, request, emitter));
        return emitter;
    }

    private void executeStream(Long userId, AiChatRequest request, SseEmitter emitter) {
        try {
            AiChatResponse response = aiChatService.stream(
                    userId,
                    request,
                    new AiChatStreamObserver() {
                        @Override
                        public void onStart(
                                String conversationId,
                                List<AiCitationResponse> citations,
                                AiConfigStatusResponse configStatus
                        ) {
                            Map<String, Object> data = new LinkedHashMap<>();
                            data.put("conversationId", conversationId);
                            data.put("mode", configStatus == null ? null : configStatus.getMode());
                            data.put("model", configStatus == null ? null : configStatus.getModel());
                            data.put("citations", citations);
                            sendSseEvent(emitter, "start", data);
                        }

                        @Override
                        public void onDelta(String delta) {
                            sendSseEvent(emitter, "delta", Map.of("delta", delta));
                        }
                    }
            );

            Map<String, Object> done = new LinkedHashMap<>();
            done.put("conversationId", response.getConversationId());
            done.put("answer", response.getAnswer());
            done.put("mode", response.getMode());
            done.put("model", response.getModel());
            done.put("traceId", response.getTraceId());
            done.put("citations", response.getCitations());
            done.put("usage", response.getUsage());
            sendSseEvent(emitter, "done", done);
            emitter.complete();
        } catch (BizException ex) {
            sendError(emitter, safeErrorMessage(ex), resolveBizErrorCode(ex));
        } catch (Exception ex) {
            sendError(emitter, safeErrorMessage(ex), 500);
        }
    }

    private int resolveBizErrorCode(BizException exception) {
        String message = exception == null ? null : exception.getMessage();
        if (message != null && message.contains("无权")) {
            return 403;
        }
        if (message != null && message.contains("不存在")) {
            return 404;
        }
        return 400;
    }

    private void sendError(SseEmitter emitter, String message, int code) {
        sendSseEvent(emitter, "error", Map.of("message", message, "code", code));
        emitter.complete();
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception ignored) {
            // 客户端可能已断开；由调用链自然结束。
        }
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

    private long resolveRequestTimeoutMillis() {
        Integer configured = aiProperties.getRequestTimeoutSeconds();
        long seconds = configured != null && configured > 0 ? configured : 60L;
        return seconds * 1000L;
    }

    @PreDestroy
    public void shutdownExecutor() {
        aiStreamExecutor.shutdownNow();
    }
}
