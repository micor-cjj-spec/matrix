package single.cjj.scheduler.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.scheduler.service.SchedulerImNotificationService;

@RestController
public class SchedulerImCallbackController {

    private final SchedulerImNotificationService notificationService;

    public SchedulerImCallbackController(SchedulerImNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/scheduler/im/callbacks")
    public ApiResponse<Boolean> callback(
            @RequestHeader("X-IM-Timestamp") String timestamp,
            @RequestHeader("X-IM-Nonce") String nonce,
            @RequestHeader("X-IM-Signature") String signature,
            @RequestHeader("X-IM-Event-Id") String eventId,
            @RequestBody String rawBody) {
        notificationService.acceptCallback(timestamp, nonce, signature, eventId, rawBody);
        return ApiResponse.success(true);
    }
}
