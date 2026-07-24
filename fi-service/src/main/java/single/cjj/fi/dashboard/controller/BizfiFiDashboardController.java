package single.cjj.fi.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.dashboard.service.BizfiFiDashboardService;
import single.cjj.fi.dashboard.vo.FinanceDashboardOverviewVO;

@RestController
@RequestMapping("/finance-dashboard")
public class BizfiFiDashboardController {

    @Autowired
    private BizfiFiDashboardService dashboardService;

    @GetMapping("/overview")
    public ApiResponse<FinanceDashboardOverviewVO> overview(
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period
    ) {
        return ApiResponse.success(dashboardService.overview(forg, period));
    }
}
