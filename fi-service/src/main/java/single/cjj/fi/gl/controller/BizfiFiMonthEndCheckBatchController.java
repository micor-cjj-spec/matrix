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
import single.cjj.fi.gl.entity.BizfiFiMonthEndCheckBatch;
import single.cjj.fi.gl.service.BizfiFiMonthEndCheckBatchService;
import single.cjj.fi.gl.vo.MonthEndBatchActionRequestVO;
import single.cjj.fi.gl.vo.MonthEndBatchCreateRequestVO;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/month-end-check-batch")
public class BizfiFiMonthEndCheckBatchController {

    @Autowired
    private BizfiFiMonthEndCheckBatchService service;

    @PostMapping
    public ApiResponse<BizfiFiMonthEndCheckBatch> create(@RequestBody(required = false) MonthEndBatchCreateRequestVO request) {
        return ApiResponse.success(service.createBatch(request));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiMonthEndCheckBatch>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "applicationStatus", required = false) String applicationStatus
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("forg", forg);
        query.put("period", period);
        query.put("applicationStatus", applicationStatus);
        return ApiResponse.success(service.list(page, size, query));
    }

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiMonthEndCheckBatch> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @PostMapping("/{fid}/submit")
    public ApiResponse<BizfiFiMonthEndCheckBatch> submit(
            @PathVariable("fid") Long fid,
            @RequestBody(required = false) MonthEndBatchActionRequestVO request
    ) {
        return ApiResponse.success(service.submit(fid, request));
    }

    @PostMapping("/{fid}/approve")
    public ApiResponse<BizfiFiMonthEndCheckBatch> approve(
            @PathVariable("fid") Long fid,
            @RequestBody(required = false) MonthEndBatchActionRequestVO request
    ) {
        return ApiResponse.success(service.approve(fid, request));
    }
}

