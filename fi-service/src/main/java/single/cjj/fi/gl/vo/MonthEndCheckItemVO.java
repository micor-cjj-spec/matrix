package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthEndCheckItemVO {
    private String code;
    private String name;
    private String category;
    private String status;
    private String severity;
    private String message;
    private String actionHint;
    private String routePath;
    private Integer relatedCount;
    private Boolean blocking;
}

