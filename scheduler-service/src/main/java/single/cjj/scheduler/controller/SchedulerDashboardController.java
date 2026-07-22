package single.cjj.scheduler.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.scheduler.service.SchedulerDashboardService;

import java.util.Map;

@RestController
@RequestMapping("/scheduler/dashboard")
public class SchedulerDashboardController {

    private final SchedulerDashboardService dashboardService;

    public SchedulerDashboardController(SchedulerDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.success(dashboardService.summary());
    }
}
