package single.cjj.scheduler.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.scheduler.entity.MatrixSchedulerAlertRecord;
import single.cjj.scheduler.service.SchedulerAlertService;

@RestController
@RequestMapping("/scheduler/alerts")
public class SchedulerAlertController {

    private final SchedulerAlertService alertService;

    public SchedulerAlertController(SchedulerAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ApiResponse<IPage<MatrixSchedulerAlertRecord>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "level", required = false) String level) {
        return ApiResponse.success(alertService.list(page, size, status, level));
    }

    @PostMapping("/{alertId}/ack")
    public ApiResponse<MatrixSchedulerAlertRecord> acknowledge(
            @PathVariable("alertId") Long alertId,
            @RequestHeader(value = "X-User-Id", required = false) String operatorId) {
        return ApiResponse.success(alertService.acknowledge(alertId, operatorId));
    }
}
