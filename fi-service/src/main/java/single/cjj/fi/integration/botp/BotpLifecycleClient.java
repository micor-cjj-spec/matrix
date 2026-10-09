package single.cjj.fi.integration.botp;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.integration.botp.BotpLifecycleContracts.TargetStatusEvent;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "botp-service",
        url = "${fi.botp-service-url:}",
        path = "/api/botp/relations"
)
public interface BotpLifecycleClient {

    @PostMapping("/target-events")
    ApiResponse<List<Map<String, Object>>> targetStatusEvent(@RequestBody TargetStatusEvent event);
}
