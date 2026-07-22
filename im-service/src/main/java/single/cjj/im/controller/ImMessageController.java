package single.cjj.im.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.im.domain.ImModels.AcceptedResponse;
import single.cjj.im.domain.ImModels.LocalNotificationRecord;
import single.cjj.im.domain.ImModels.MessageStatusResponse;
import single.cjj.im.domain.ImModels.PagedResult;
import single.cjj.im.domain.ImModels.SendMessageRequest;
import single.cjj.im.domain.ImModels.TemplateRecord;
import single.cjj.im.domain.ImModels.TemplateUpsertRequest;
import single.cjj.im.security.OpenApiSignatureFilter;
import single.cjj.im.service.MessageCommandService;

import java.util.List;
import java.util.Map;

@RestController
public class ImMessageController {

    private final MessageCommandService messageService;

    public ImMessageController(MessageCommandService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/open-api/v1/messages/send")
    public ApiResponse<AcceptedResponse> send(
            @RequestAttribute(OpenApiSignatureFilter.APP_CODE_ATTRIBUTE) String appCode,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success("消息已可靠受理", messageService.acceptDirect(appCode, request));
    }

    @PostMapping("/open-api/v1/messages/template-send")
    public ApiResponse<AcceptedResponse> templateSend(
            @RequestAttribute(OpenApiSignatureFilter.APP_CODE_ATTRIBUTE) String appCode,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success("模板消息已可靠受理", messageService.acceptTemplate(appCode, request));
    }

    @GetMapping("/open-api/v1/messages/{messageNo}")
    public ApiResponse<MessageStatusResponse> queryStatus(
            @RequestAttribute(OpenApiSignatureFilter.APP_CODE_ATTRIBUTE) String appCode,
            @PathVariable("messageNo") String messageNo) {
        return ApiResponse.success(messageService.queryStatus(appCode, messageNo));
    }

    @PostMapping("/im/templates")
    public ApiResponse<TemplateRecord> upsertTemplate(@Valid @RequestBody TemplateUpsertRequest request) {
        return ApiResponse.success(messageService.upsertTemplate(request));
    }

    @GetMapping("/im/templates")
    public ApiResponse<List<TemplateRecord>> listTemplates() {
        return ApiResponse.success(messageService.listTemplates());
    }

    @GetMapping("/im/notifications")
    public ApiResponse<PagedResult<LocalNotificationRecord>> listNotifications(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(value = "readStatus", required = false) String readStatus,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(messageService.listNotifications(userId, readStatus, page, size));
    }

    @GetMapping("/im/notifications/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.success(Map.of("count", messageService.unreadCount(userId)));
    }

    @PostMapping("/im/notifications/{notificationId}/read")
    public ApiResponse<Boolean> markRead(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("notificationId") String notificationId) {
        return ApiResponse.success(messageService.markRead(notificationId, userId));
    }

    @PostMapping("/im/notifications/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.success(Map.of("updated", messageService.markAllRead(userId)));
    }
}
