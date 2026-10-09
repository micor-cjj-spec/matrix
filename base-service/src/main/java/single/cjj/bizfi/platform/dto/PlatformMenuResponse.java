package single.cjj.bizfi.platform.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlatformMenuResponse {
    private Long id;
    private Long parentId;
    private String appCode;
    private String moduleCode;
    private String menuCode;
    private String name;
    private String title;
    private String desc;
    private String description;
    private String summary;
    private String eyebrow;
    private String menuType;
    private String path;
    private String routePath;
    private String iconKey;
    private String status;
    private Boolean available;
    private Boolean ready;
    private List<PlatformMenuResponse> children = new ArrayList<>();
}
