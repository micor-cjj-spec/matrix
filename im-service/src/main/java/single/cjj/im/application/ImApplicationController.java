package single.cjj.im.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.im.application.ImApplicationService.ApplicationSecretResponse;
import single.cjj.im.application.ImApplicationService.ApplicationUpsertRequest;
import single.cjj.im.application.ImApplicationService.ApplicationView;

import java.util.List;

@RestController
public class ImApplicationController {

    private final ImApplicationService applicationService;

    public ImApplicationController(ImApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/im/applications")
    public ApiResponse<ApplicationSecretResponse> upsert(@RequestBody ApplicationUpsertRequest request) {
        return ApiResponse.success("IM 应用配置已保存", applicationService.upsert(request));
    }

    @PostMapping("/im/applications/{appCode}/rotate-secret")
    public ApiResponse<ApplicationSecretResponse> rotateSecret(@PathVariable("appCode") String appCode) {
        return ApiResponse.success("IM 应用密钥已轮换", applicationService.rotateSecret(appCode));
    }

    @GetMapping("/im/applications")
    public ApiResponse<List<ApplicationView>> list() {
        return ApiResponse.success(applicationService.list());
    }
}
