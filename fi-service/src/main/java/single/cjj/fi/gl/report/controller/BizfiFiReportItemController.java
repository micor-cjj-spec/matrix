package single.cjj.fi.gl.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.report.entity.BizfiFiReportItem;
import single.cjj.fi.gl.report.service.BizfiFiReportItemService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report-item")
public class BizfiFiReportItemController {

    @Autowired
    private BizfiFiReportItemService service;

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiReportItem> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @PostMapping
    public ApiResponse<BizfiFiReportItem> add(@RequestBody BizfiFiReportItem item) {
        return ApiResponse.success(service.add(item));
    }

    @PutMapping
    public ApiResponse<BizfiFiReportItem> update(@RequestBody BizfiFiReportItem item) {
        return ApiResponse.success(service.update(item));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiReportItem>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "ftemplateId", required = false) Long ftemplateId,
            @RequestParam(value = "fcode", required = false) String fcode,
            @RequestParam(value = "fname", required = false) String fname
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("ftemplateId", ftemplateId);
        query.put("fcode", fcode);
        query.put("fname", fname);
        return ApiResponse.success(service.list(page, size, query));
    }

    @GetMapping("/tree")
    public ApiResponse<List<BizfiFiReportItem>> tree(@RequestParam("ftemplateId") Long ftemplateId) {
        return ApiResponse.success(service.listByTemplateId(ftemplateId));
    }
}

