package single.cjj.fi.ar.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.service.BizfiFiArapDocService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/arap-doc")
public class BizfiFiArapDocController {

    @Autowired
    private BizfiFiArapDocService service;

    @PostMapping
    public ApiResponse<BizfiFiArapDoc> create(@RequestBody BizfiFiArapDoc doc) { return ApiResponse.success(service.create(doc)); }

    @PutMapping
    public ApiResponse<BizfiFiArapDoc> update(@RequestBody BizfiFiArapDoc doc) { return ApiResponse.success(service.update(doc)); }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) { return ApiResponse.success(service.deleteDraft(fid)); }

    @PostMapping("/submit/{fid}")
    public ApiResponse<BizfiFiArapDoc> submit(@PathVariable("fid") Long fid) { return ApiResponse.success(service.submit(fid)); }

    @PostMapping("/submit/by-number")
    public ApiResponse<BizfiFiArapDoc> submitByNumber(@RequestParam("number") String number) {
        return ApiResponse.success(service.submitByNumber(number));
    }

    @PostMapping("/audit/{fid}")
    public ApiResponse<BizfiFiArapDoc> audit(@PathVariable("fid") Long fid,
                                             @RequestParam(value = "operator", required = false) String operator) { return ApiResponse.success(service.audit(fid, operator)); }

    @PostMapping("/audit/by-number")
    public ApiResponse<BizfiFiArapDoc> auditByNumber(@RequestParam("number") String number,
                                                      @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.auditByNumber(number, operator));
    }

    @PostMapping("/reject/{fid}")
    public ApiResponse<BizfiFiArapDoc> reject(@PathVariable("fid") Long fid,
                                              @RequestParam(value = "operator", required = false) String operator) { return ApiResponse.success(service.reject(fid, operator)); }

    @PostMapping("/reject/by-number")
    public ApiResponse<BizfiFiArapDoc> rejectByNumber(@RequestParam("number") String number,
                                                       @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.rejectByNumber(number, operator));
    }

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiArapDoc> detail(@PathVariable("fid") Long fid) { return ApiResponse.success(service.detail(fid)); }

    @GetMapping("/list")
    public ApiResponse<IPage<BizfiFiArapDoc>> list(@RequestParam("docType") String docType,
                                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                                   @RequestParam(value = "size", defaultValue = "10") int size,
                                                   @RequestParam(value = "number", required = false) String number,
                                                   @RequestParam(value = "status", required = false) String status,
                                                   @RequestParam(value = "counterparty", required = false) String counterparty,
                                                   @RequestParam(value = "startDate", required = false) String startDate,
                                                   @RequestParam(value = "endDate", required = false) String endDate,
                                                   @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
                                                   @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount) {
        return ApiResponse.success(service.list(docType, page, size, number, status, counterparty, startDate, endDate, minAmount, maxAmount));
    }

    @PostMapping("/voucher/{fid}")
    public ApiResponse<BizfiFiArapDoc> generateVoucher(@PathVariable("fid") Long fid,
                                                        @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.generateVoucher(fid, operator));
    }

    @PostMapping("/voucher/by-number")
    public ApiResponse<BizfiFiArapDoc> generateVoucherByNumber(@RequestParam("number") String number,
                                                                @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.generateVoucherByNumber(number, operator));
    }

    @GetMapping("/by-voucher")
    public ApiResponse<List<BizfiFiArapDoc>> listByVoucher(@RequestParam(value = "voucherId", required = false) Long voucherId,
                                                            @RequestParam(value = "voucherNumber", required = false) String voucherNumber) {
        return ApiResponse.success(service.listByVoucher(voucherId, voucherNumber));
    }
}
