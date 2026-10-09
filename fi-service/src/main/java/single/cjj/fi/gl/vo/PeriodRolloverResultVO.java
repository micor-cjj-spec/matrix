package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import single.cjj.fi.gl.entity.BizfiFiAccountingPeriod;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCloseExecution;
import single.cjj.fi.gl.entity.BizfiFiOrgFinanceConfig;
import single.cjj.fi.gl.entity.BizfiFiPeriodRollover;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodRolloverResultVO {
    private BizfiFiPeriodRollover rollover;
    private BizfiFiMonthEndCloseExecution closeExecution;
    private BizfiFiAccountingPeriod nextPeriod;
    private BizfiFiOrgFinanceConfig orgFinanceConfig;
}
