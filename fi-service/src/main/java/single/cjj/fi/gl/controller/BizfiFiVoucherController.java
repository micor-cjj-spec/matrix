package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.service.BizfiFiArapDocService;
import single.cjj.fi.gl.dto.BizfiFiVoucherOcrImportRequest;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.service.BizfiFiVoucherOcrService;
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

    @Autowired
    private BizfiFiArapDocService arapDocService;

    @Autowired
    private BizfiFiVoucherOcrService voucherOcrService;

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

    /** 批量过账（同步） */
    @PostMapping("/post/batch")
    public ApiResponse<Map<String, Object>> postBatch(@RequestBody List<Long> fids,
                                                       @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.postBatch(fids, operator));
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

    /** OCR解析凭证图片/PDF（MVP） */
    @PostMapping("/import/ocr/parse")
    public ApiResponse<Map<String, Object>> ocrParse(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(voucherOcrService.parseFile(file));
    }

    /** OCR确认导入（凭证+分录一体导入） */
    @PostMapping("/import/ocr/confirm")
    public ApiResponse<BizfiFiVoucher> ocrConfirm(@RequestBody BizfiFiVoucherOcrImportRequest req) {
        if (req == null || req.getVoucher() == null) {
            throw new single.cjj.bizfi.exception.BizException("凭证数据不能为空");
        }
        BizfiFiVoucher saved = service.saveDraft(req.getVoucher());
        List<BizfiFiVoucherLine> lines = req.getLines();
        if (lines != null && !lines.isEmpty()) {
            service.saveLines(saved.getFid(), lines);
        }
        return ApiResponse.success(service.get(saved.getFid()));
    }

    /** 详情 */
    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiVoucher> detail(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    /** 查询来源单据（AR/AP） */
    @GetMapping("/{fid}/source-docs")
    public ApiResponse<List<BizfiFiArapDoc>> sourceDocs(@PathVariable("fid") Long fid) {
        BizfiFiVoucher voucher = service.get(fid);
        if (voucher == null) {
            return ApiResponse.success(java.util.Collections.emptyList());
        }
        return ApiResponse.success(arapDocService.listByVoucher(fid, voucher.getFnumber()));
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
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("number", number);
        query.put("summary", summary);
        query.put("status", status);
        query.put("startDate", startDate);
        query.put("endDate", endDate);
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
