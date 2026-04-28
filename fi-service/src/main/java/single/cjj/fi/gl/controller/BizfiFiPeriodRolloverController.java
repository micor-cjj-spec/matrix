package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.entity.BizfiFiPeriodRollover;
import single.cjj.fi.gl.service.BizfiFiPeriodRolloverService;
import single.cjj.fi.gl.vo.PeriodRolloverRequestVO;
import single.cjj.fi.gl.vo.PeriodRolloverResultVO;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/period-rollover")
public class BizfiFiPeriodRolloverController {

    @Autowired
    private BizfiFiPeriodRolloverService service;

    @PostMapping("/from-close-execution/{executionId}")
    public ApiResponse<PeriodRolloverResultVO> rolloverFromCloseExecution(
            @PathVariable("executionId") Long executionId,
            @RequestBody(required = false) PeriodRolloverRequestVO request
    ) {
        return ApiResponse.success(service.rolloverFromCloseExecution(executionId, request));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiPeriodRollover>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "fromPeriod", required = false) String fromPeriod,
            @RequestParam(value = "toPeriod", required = false) String toPeriod,
            @RequestParam(value = "closeExecutionId", required = false) Long closeExecutionId,
            @RequestParam(value = "rolloverStatus", required = false) String rolloverStatus
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("forg", forg);
        query.put("fromPeriod", fromPeriod);
        query.put("toPeriod", toPeriod);
        query.put("closeExecutionId", closeExecutionId);
        query.put("rolloverStatus", rolloverStatus);
        return ApiResponse.success(service.list(page, size, query));
    }
}
