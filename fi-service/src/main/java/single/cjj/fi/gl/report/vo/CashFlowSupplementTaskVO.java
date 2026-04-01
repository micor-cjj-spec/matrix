package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowSupplementTaskVO {
    private String code;
    private String name;
    private String status;
    private String message;
    private String actionHint;
}
