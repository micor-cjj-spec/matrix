package single.cjj.fi.gl.config.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.config.entity.BizfiFiBaseConfigItem;
import single.cjj.fi.gl.config.service.BizfiFiBaseConfigItemService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/base-config-item")
public class BizfiFiBaseConfigItemController {

    @Autowired
    private BizfiFiBaseConfigItemService service;

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiBaseConfigItem> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @PostMapping
    public ApiResponse<BizfiFiBaseConfigItem> add(@RequestBody BizfiFiBaseConfigItem item) {
        return ApiResponse.success(service.add(item));
    }

    @PutMapping
    public ApiResponse<BizfiFiBaseConfigItem> update(@RequestBody BizfiFiBaseConfigItem item) {
        return ApiResponse.success(service.update(item));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiBaseConfigItem>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "fcategory", required = false) String fcategory,
            @RequestParam(value = "fstatus", required = false) String fstatus,
            @RequestParam(value = "fcode", required = false) String fcode,
            @RequestParam(value = "fname", required = false) String fname
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("forg", forg);
        query.put("fcategory", fcategory);
        query.put("fstatus", fstatus);
        query.put("fcode", fcode);
        query.put("fname", fname);
        return ApiResponse.success(service.list(page, size, query));
    }
}
