package single.cjj.fi.gl.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.report.entity.BizfiFiCashflowItem;
import single.cjj.fi.gl.report.service.BizfiFiCashflowItemService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cashflow-item")
public class BizfiFiCashflowItemController {

    @Autowired
    private BizfiFiCashflowItemService service;

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiCashflowItem> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @PostMapping
    public ApiResponse<BizfiFiCashflowItem> add(@RequestBody BizfiFiCashflowItem item) {
        return ApiResponse.success(service.add(item));
    }

    @PutMapping
    public ApiResponse<BizfiFiCashflowItem> update(@RequestBody BizfiFiCashflowItem item) {
        return ApiResponse.success(service.update(item));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiCashflowItem>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "fcode", required = false) String fcode,
            @RequestParam(value = "fname", required = false) String fname,
            @RequestParam(value = "fcategory", required = false) String fcategory,
            @RequestParam(value = "fstatus", required = false) String fstatus
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("fcode", fcode);
        query.put("fname", fname);
        query.put("fcategory", fcategory);
        query.put("fstatus", fstatus);
        return ApiResponse.success(service.list(page, size, query));
    }
}
