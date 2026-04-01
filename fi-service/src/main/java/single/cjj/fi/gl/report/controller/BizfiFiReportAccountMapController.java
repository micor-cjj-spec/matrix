package single.cjj.fi.gl.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.report.entity.BizfiFiReportAccountMap;
import single.cjj.fi.gl.report.service.BizfiFiReportAccountMapService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/report-account-map")
public class BizfiFiReportAccountMapController {

    @Autowired
    private BizfiFiReportAccountMapService service;

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiReportAccountMap> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @PostMapping
    public ApiResponse<BizfiFiReportAccountMap> add(@RequestBody BizfiFiReportAccountMap accountMap) {
        return ApiResponse.success(service.add(accountMap));
    }

    @PutMapping
    public ApiResponse<BizfiFiReportAccountMap> update(@RequestBody BizfiFiReportAccountMap accountMap) {
        return ApiResponse.success(service.update(accountMap));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiReportAccountMap>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "ftemplateId", required = false) Long ftemplateId,
            @RequestParam(value = "fitemId", required = false) Long fitemId,
            @RequestParam(value = "faccountId", required = false) Long faccountId
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("ftemplateId", ftemplateId);
        query.put("fitemId", fitemId);
        query.put("faccountId", faccountId);
        return ApiResponse.success(service.list(page, size, query));
    }
}

