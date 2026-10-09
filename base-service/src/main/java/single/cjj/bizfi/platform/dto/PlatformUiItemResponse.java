package single.cjj.bizfi.platform.dto;

import lombok.Data;

@Data
public class PlatformUiItemResponse {
    private String key;
    private String name;
    private String label;
    private String title;
    private String desc;
    private String description;
    private String detail;
    private String value;
    private String hint;
    private String tag;
    private String type;
    private String priority;
    private String status;
    private String meta;
    private String time;
    private String path;
    private String routePath;
    private String iconKey;
    private String accent;
    private Boolean available;
    private Boolean newPage;
    private Boolean featured;
    private Boolean primary;
    private Boolean ready;
}
