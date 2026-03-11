package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.service.BizfiFiVoucherService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务凭证接口
 */
@RestController
@RequestMapping("/voucher")
public class BizfiFiVoucherController {

    @Autowired
    private BizfiFiVoucherService service;

    /** 保存草稿 */
    @PostMapping("/save")
    public ApiResponse<BizfiFiVoucher> saveDraft(@RequestBody BizfiFiVoucher voucher) {
        return ApiResponse.success(service.saveDraft(voucher));
    }

    /** 新增兼容（等同保存草稿） */
    @PostMapping
    public ApiResponse<BizfiFiVoucher> add(@RequestBody BizfiFiVoucher voucher) {
        return ApiResponse.success(service.saveDraft(voucher));
    }

    /** 修改草稿 */
    @PutMapping
    public ApiResponse<BizfiFiVoucher> update(@RequestBody BizfiFiVoucher voucher) {
        return ApiResponse.success(service.updateDraft(voucher));
    }

    /** 提交 */
    @PostMapping("/submit/{fid}")
    public ApiResponse<BizfiFiVoucher> submit(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.submit(fid));
    }

    /** 审核 */
    @PostMapping("/audit/{fid}")
    public ApiResponse<BizfiFiVoucher> audit(@PathVariable("fid") Long fid,
                                             @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.audit(fid, operator));
    }

    /** 过账 */
    @PostMapping("/post/{fid}")
    public ApiResponse<BizfiFiVoucher> post(@PathVariable("fid") Long fid,
                                            @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.post(fid, operator));
    }

    /** 驳回 */
    @PostMapping("/reject/{fid}")
    public ApiResponse<BizfiFiVoucher> reject(@PathVariable("fid") Long fid,
                                              @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.reject(fid, operator));
    }

    /** 冲销 */
    @PostMapping("/reverse/{fid}")
    public ApiResponse<BizfiFiVoucher> reverse(@PathVariable("fid") Long fid,
                                               @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.reverse(fid, operator));
    }

    /** 详情 */
    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiVoucher> detail(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    /** 删除草稿 */
    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.deleteDraft(fid));
    }

    /** 分页/条件查询列表 */
    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiVoucher>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "number", required = false) String number,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "status", required = false) String status
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("number", number);
        query.put("summary", summary);
        query.put("status", status);
        return ApiResponse.success(service.list(page, size, query));
    }

    /** 查询分录 */
    @GetMapping("/{fid}/lines")
    public ApiResponse<List<BizfiFiVoucherLine>> listLines(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.listLines(fid));
    }

    /** 保存分录（覆盖） */
    @PutMapping("/{fid}/lines")
    public ApiResponse<Boolean> saveLines(@PathVariable("fid") Long fid,
                                          @RequestBody List<BizfiFiVoucherLine> lines) {
        return ApiResponse.success(service.saveLines(fid, lines));
    }
}
