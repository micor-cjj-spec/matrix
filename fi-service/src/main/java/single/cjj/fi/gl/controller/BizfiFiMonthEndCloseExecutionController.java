package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCloseExecution;
import single.cjj.fi.gl.service.BizfiFiMonthEndCloseExecutionService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/month-end-close-execution", "/period-process/month-end-close-execution"})
public class BizfiFiMonthEndCloseExecutionController {

    @Autowired
    private BizfiFiMonthEndCloseExecutionService service;

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiMonthEndCloseExecution>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "batchId", required = false) Long batchId,
            @RequestParam(value = "executionStatus", required = false) String executionStatus
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("forg", forg);
        query.put("period", period);
        query.put("batchId", batchId);
        query.put("executionStatus", executionStatus);
        return ApiResponse.success(service.list(page, size, query));
    }
}
