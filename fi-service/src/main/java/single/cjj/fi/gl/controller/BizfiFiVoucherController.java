package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.service.BizfiFiVoucherService;

import java.util.HashMap;
import java.util.Map;

/**
 * 财务凭证接口
 */
@RestController
@RequestMapping("/voucher")
public class BizfiFiVoucherController {

    @Autowired
    private BizfiFiVoucherService service;

    /** 新增凭证 */
    @PostMapping
    public ApiResponse<BizfiFiVoucher> add(@RequestBody BizfiFiVoucher voucher) {
        return ApiResponse.success(service.add(voucher));
    }

    /** 修改凭证 */
    @PutMapping
    public ApiResponse<BizfiFiVoucher> update(@RequestBody BizfiFiVoucher voucher) {
        return ApiResponse.success(service.update(voucher));
    }

    /** 分页/条件查询列表 */
    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiVoucher>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "number", required = false) String number,
            @RequestParam(value = "summary", required = false) String summary
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("number", number);
        query.put("summary", summary);
        return ApiResponse.success(service.list(page, size, query));
    }
}
