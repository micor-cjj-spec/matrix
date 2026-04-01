package single.cjj.bizfi.ai.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.ai.dto.*;
import single.cjj.bizfi.ai.entity.BizfiAiConversation;
import single.cjj.bizfi.ai.entity.BizfiAiMessage;
import single.cjj.bizfi.ai.service.AiChatService;
import single.cjj.bizfi.ai.service.AiConversationService;
import single.cjj.bizfi.ai.service.AiCurrentUserService;
import single.cjj.bizfi.ai.service.AiFeedbackService;
import single.cjj.bizfi.entity.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiAssistantFacadeController {

    @Autowired
    private AiCurrentUserService currentUserService;

    @Autowired
    private AiConversationService conversationService;

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private AiFeedbackService aiFeedbackService;

    @PostMapping("/conversations")
    public ApiResponse<AiConversationCreateResponse> createConversation(@RequestBody(required = false) AiConversationCreateRequest req) {
        Long userId = currentUserService.currentUserId();
        String title = req != null ? req.getTitle() : null;
        String scene = req != null ? req.getScene() : null;
        BizfiAiConversation conversation = conversationService.createConversation(userId, title, scene);
        return ApiResponse.success(new AiConversationCreateResponse(
                conversation.getFconversationid(),
                conversation.getFtitle()
        ));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<AiConversationMessagesResponse> getMessages(@PathVariable String conversationId) {
        Long userId = currentUserService.currentUserId();
        List<BizfiAiMessage> dbMessages = conversationService.listMessages(userId, conversationId);
        List<AiMessageResponse> messages = dbMessages.stream()
                .map(item -> new AiMessageResponse(item.getFrole(), item.getFcontent()))
                .toList();
        return ApiResponse.success(new AiConversationMessagesResponse(conversationId, messages));
    }

    @GetMapping("/config/status")
    public ApiResponse<AiConfigStatusResponse> configStatus() {
        return ApiResponse.success(aiChatService.configStatus());
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody AiChatRequest req) {
        Long userId = currentUserService.currentUserId();
        return ApiResponse.success(aiChatService.chat(userId, req));
    }

    @PostMapping("/feedback")
    public ApiResponse<Boolean> feedback(@RequestBody(required = false) AiFeedbackRequest request) {
        Long userId = currentUserService.currentUserId();
        return ApiResponse.success(aiFeedbackService.saveFeedback(userId, request));
    }
}
