package single.cjj.bizfi.platform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.platform.dto.PlatformMenuResponse;
import single.cjj.bizfi.platform.dto.PlatformModuleHubResponse;
import single.cjj.bizfi.platform.dto.PlatformUiItemResponse;
import single.cjj.bizfi.platform.dto.PlatformWorkbenchResponse;
import single.cjj.bizfi.platform.service.PlatformConfigService;

import java.util.List;

@RestController
@RequestMapping("/platform")
public class PlatformConfigController {

    @Autowired
    private PlatformConfigService platformConfigService;

    @GetMapping("/workbench")
    public ApiResponse<PlatformWorkbenchResponse> getWorkbench() {
        return ApiResponse.success(platformConfigService.getWorkbench());
    }

    @GetMapping("/apps")
    public ApiResponse<List<PlatformUiItemResponse>> getApps() {
        return ApiResponse.success(platformConfigService.getApps());
    }

    @GetMapping("/menu-tree")
    public ApiResponse<List<PlatformMenuResponse>> getMenuTree(
            @RequestParam(value = "appCode", required = false) String appCode
    ) {
        return ApiResponse.success(platformConfigService.getMenuTree(appCode));
    }

    @GetMapping("/module-hub")
    public ApiResponse<PlatformModuleHubResponse> getModuleHub(
            @RequestParam("appCode") String appCode,
            @RequestParam("moduleCode") String moduleCode
    ) {
        return ApiResponse.success(platformConfigService.getModuleHub(appCode, moduleCode));
    }
}
