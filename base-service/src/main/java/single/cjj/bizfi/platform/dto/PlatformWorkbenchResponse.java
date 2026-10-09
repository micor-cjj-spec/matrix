package single.cjj.bizfi.platform.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlatformWorkbenchResponse {
    private List<PlatformUiItemResponse> apps = new ArrayList<>();
    private List<PlatformUiItemResponse> heroMetrics = new ArrayList<>();
    private List<PlatformUiItemResponse> todos = new ArrayList<>();
    private List<PlatformUiItemResponse> recentItems = new ArrayList<>();
    private List<PlatformUiItemResponse> notices = new ArrayList<>();
    private List<PlatformUiItemResponse> quickActions = new ArrayList<>();
}
