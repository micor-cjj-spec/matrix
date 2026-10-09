package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthEndStepVO {
    private Integer orderNo;
    private String stepCode;
    private String stepName;
    private String status;
    private String summary;
    private String actionHint;
    private String routePath;
    private Integer blockingCount;
    private Integer warningCount;
}

