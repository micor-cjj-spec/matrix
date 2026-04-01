package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodMonitorModuleVO {
    private String moduleCode;
    private String moduleName;
    private String status;
    private String summary;
    private String actionHint;
    private Integer matchedVoucherCount;
    private Integer pendingVoucherCount;
}
