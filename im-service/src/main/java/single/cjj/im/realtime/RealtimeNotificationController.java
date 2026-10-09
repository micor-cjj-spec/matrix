package single.cjj.im.realtime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.im.realtime.RealtimeModels.SyncResponse;

@RestController
public class RealtimeNotificationController {
    private final RealtimeNotificationService realtimeService;
    public RealtimeNotificationController(RealtimeNotificationService realtimeService) { this.realtimeService = realtimeService; }
    @GetMapping("/im/sync")
    public ApiResponse<SyncResponse> sync(@RequestHeader("X-User-Id") String userId, @RequestHeader(value="X-Tenant-Id",defaultValue="default") String tenantId, @RequestParam(value="afterVersion",defaultValue="0") long afterVersion, @RequestParam(value="limit",defaultValue="100") int limit) { return ApiResponse.success(realtimeService.sync(tenantId,userId,afterVersion,limit)); }
}
