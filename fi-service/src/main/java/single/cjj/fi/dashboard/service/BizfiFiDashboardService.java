package single.cjj.fi.dashboard.service;

import single.cjj.fi.dashboard.vo.FinanceDashboardOverviewVO;

public interface BizfiFiDashboardService {

    FinanceDashboardOverviewVO overview(Long forg, String period);
}
