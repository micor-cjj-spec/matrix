package single.cjj.bizfi.platform.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlatformModuleGroupResponse {
    private String name;
    private String summary;
    private String eyebrow;
    private String iconKey;
    private List<PlatformUiItemResponse> modules = new ArrayList<>();
}
