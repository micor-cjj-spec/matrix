package single.cjj.bizfi.platform.service;

import single.cjj.bizfi.platform.dto.PlatformMenuResponse;
import single.cjj.bizfi.platform.dto.PlatformModuleHubResponse;
import single.cjj.bizfi.platform.dto.PlatformUiItemResponse;
import single.cjj.bizfi.platform.dto.PlatformWorkbenchResponse;

import java.util.List;

public interface PlatformConfigService {
    PlatformWorkbenchResponse getWorkbench();

    List<PlatformUiItemResponse> getApps();

    List<PlatformMenuResponse> getMenuTree(String appCode);

    PlatformModuleHubResponse getModuleHub(String appCode, String moduleCode);
}
