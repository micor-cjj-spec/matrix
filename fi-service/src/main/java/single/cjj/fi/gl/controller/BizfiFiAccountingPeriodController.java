package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.entity.BizfiFiAccountingPeriod;
import single.cjj.fi.gl.service.BizfiFiAccountingPeriodService;

import java.util.HashMap;
import java.util.Map;

/**
 * 会计期间接口
 */
@RestController
@RequestMapping("/accounting-period")
public class BizfiFiAccountingPeriodController {

    @Autowired
    private BizfiFiAccountingPeriodService service;

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiAccountingPeriod> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @PostMapping
    public ApiResponse<BizfiFiAccountingPeriod> add(@RequestBody BizfiFiAccountingPeriod period) {
        return ApiResponse.success(service.add(period));
    }

    @PutMapping
    public ApiResponse<BizfiFiAccountingPeriod> update(@RequestBody BizfiFiAccountingPeriod period) {
        return ApiResponse.success(service.update(period));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    @PostMapping("/{fid}/close")
    public ApiResponse<BizfiFiAccountingPeriod> close(@PathVariable("fid") Long fid,
                                                      @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.close(fid, operator));
    }

    @PostMapping("/{fid}/reopen")
    public ApiResponse<BizfiFiAccountingPeriod> reopen(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.reopen(fid));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiAccountingPeriod>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "fstatus", required = false) String fstatus,
            @RequestParam(value = "fyear", required = false) Integer fyear,
            @RequestParam(value = "fperiod", required = false) String fperiod
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("forg", forg);
        query.put("fstatus", fstatus);
        query.put("fyear", fyear);
        query.put("fperiod", fperiod);
        return ApiResponse.success(service.list(page, size, query));
    }
}
