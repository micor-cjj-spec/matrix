package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import single.cjj.fi.gl.entity.BizfiFiAccountingPeriod;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCheckBatch;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCloseExecution;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthEndCloseExecutionResultVO {
    private BizfiFiMonthEndCloseExecution execution;
    private BizfiFiMonthEndCheckBatch batch;
    private BizfiFiAccountingPeriod accountingPeriod;
    private MonthEndWorkbenchResultVO workbench;
}
