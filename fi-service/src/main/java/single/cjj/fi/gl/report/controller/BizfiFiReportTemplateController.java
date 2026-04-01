package single.cjj.fi.gl.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.report.entity.BizfiFiReportTemplate;
import single.cjj.fi.gl.report.service.BizfiFiReportTemplateService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/report-template")
public class BizfiFiReportTemplateController {

    @Autowired
    private BizfiFiReportTemplateService service;

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiReportTemplate> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @PostMapping
    public ApiResponse<BizfiFiReportTemplate> add(@RequestBody BizfiFiReportTemplate template) {
        return ApiResponse.success(service.add(template));
    }

    @PutMapping
    public ApiResponse<BizfiFiReportTemplate> update(@RequestBody BizfiFiReportTemplate template) {
        return ApiResponse.success(service.update(template));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiReportTemplate>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "fcode", required = false) String fcode,
            @RequestParam(value = "fname", required = false) String fname,
            @RequestParam(value = "ftype", required = false) String ftype,
            @RequestParam(value = "fstatus", required = false) String fstatus,
            @RequestParam(value = "forg", required = false) Long forg
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("fcode", fcode);
        query.put("fname", fname);
        query.put("ftype", ftype);
        query.put("fstatus", fstatus);
        query.put("forg", forg);
        return ApiResponse.success(service.list(page, size, query));
    }
}

