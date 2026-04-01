package single.cjj.fi.gl.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.report.entity.BizfiFiReportRule;
import single.cjj.fi.gl.report.service.BizfiFiReportRuleService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/report-rule")
public class BizfiFiReportRuleController {

    @Autowired
    private BizfiFiReportRuleService service;

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiReportRule> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @PostMapping
    public ApiResponse<BizfiFiReportRule> add(@RequestBody BizfiFiReportRule rule) {
        return ApiResponse.success(service.add(rule));
    }

    @PutMapping
    public ApiResponse<BizfiFiReportRule> update(@RequestBody BizfiFiReportRule rule) {
        return ApiResponse.success(service.update(rule));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiReportRule>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "fitemId", required = false) Long fitemId,
            @RequestParam(value = "fruleType", required = false) String fruleType,
            @RequestParam(value = "fstatus", required = false) String fstatus
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("fitemId", fitemId);
        query.put("fruleType", fruleType);
        query.put("fstatus", fstatus);
        return ApiResponse.success(service.list(page, size, query));
    }
}

