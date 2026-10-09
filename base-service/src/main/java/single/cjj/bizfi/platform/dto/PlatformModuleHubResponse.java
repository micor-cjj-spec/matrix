package single.cjj.bizfi.platform.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlatformModuleHubResponse {
    private String appCode;
    private String moduleCode;
    private List<PlatformUiItemResponse> stats = new ArrayList<>();
    private List<PlatformUiItemResponse> actions = new ArrayList<>();
    private List<PlatformUiItemResponse> topActions = new ArrayList<>();
    private List<PlatformUiItemResponse> focusItems = new ArrayList<>();
    private List<PlatformUiItemResponse> shortcuts = new ArrayList<>();
    private List<PlatformModuleGroupResponse> groups = new ArrayList<>();
}
