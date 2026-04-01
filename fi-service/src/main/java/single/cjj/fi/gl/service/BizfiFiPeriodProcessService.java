package single.cjj.fi.gl.service;

import single.cjj.fi.gl.vo.PeriodMonitorCenterResultVO;
import single.cjj.fi.gl.vo.PeriodProcessResultVO;

public interface BizfiFiPeriodProcessService {
    PeriodProcessResultVO profitLoss(Long forg, String period);

    PeriodProcessResultVO autoTransfer(Long forg, String period);

    PeriodProcessResultVO fxRevalue(Long forg, String period);

    PeriodProcessResultVO voucherAmortization(Long forg, String period);

    PeriodProcessResultVO closeBooks(Long forg, String period);

    PeriodMonitorCenterResultVO monitorCenter(Long forg, String period);
}
