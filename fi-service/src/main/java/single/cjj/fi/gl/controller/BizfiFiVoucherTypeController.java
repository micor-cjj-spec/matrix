package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.entity.BizfiFiVoucherType;
import single.cjj.fi.gl.service.BizfiFiVoucherTypeService;

import java.util.HashMap;
import java.util.Map;

/**
 * 凭证字接口
 */
@RestController
@RequestMapping("/voucher-type")
public class BizfiFiVoucherTypeController {

    @Autowired
    private BizfiFiVoucherTypeService service;

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiVoucherType> get(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @PostMapping
    public ApiResponse<BizfiFiVoucherType> add(@RequestBody BizfiFiVoucherType voucherType) {
        return ApiResponse.success(service.add(voucherType));
    }

    @PutMapping
    public ApiResponse<BizfiFiVoucherType> update(@RequestBody BizfiFiVoucherType voucherType) {
        return ApiResponse.success(service.update(voucherType));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.delete(fid));
    }

    @PostMapping("/{fid}/enable")
    public ApiResponse<BizfiFiVoucherType> enable(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.enable(fid));
    }

    @PostMapping("/{fid}/disable")
    public ApiResponse<BizfiFiVoucherType> disable(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.disable(fid));
    }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiVoucherType>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "fstatus", required = false) String fstatus,
            @RequestParam(value = "fcode", required = false) String fcode,
            @RequestParam(value = "fname", required = false) String fname
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("forg", forg);
        query.put("fstatus", fstatus);
        query.put("fcode", fcode);
        query.put("fname", fname);
        return ApiResponse.success(service.list(page, size, query));
    }
}
