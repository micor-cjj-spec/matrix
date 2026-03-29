package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.service.BizfiFiArapDocService;
import single.cjj.fi.gl.dto.BizfiFiVoucherOcrImportRequest;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.service.BizfiFiVoucherAnalysisService;
import single.cjj.fi.gl.service.BizfiFiVoucherOcrService;
import single.cjj.fi.gl.service.BizfiFiVoucherService;
import single.cjj.fi.gl.vo.VoucherCarryListResultVO;
import single.cjj.fi.gl.vo.VoucherSummaryResultVO;

import java.util.Collections;
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

    @Autowired
    private BizfiFiVoucherAnalysisService voucherAnalysisService;

    @PostMapping("/save")
    public ApiResponse<BizfiFiVoucher> saveDraft(@RequestBody BizfiFiVoucher voucher) {
        return ApiResponse.success(service.saveDraft(voucher));
    }

    @PostMapping
    public ApiResponse<BizfiFiVoucher> add(@RequestBody BizfiFiVoucher voucher) {
        return ApiResponse.success(service.saveDraft(voucher));
    }

    @PutMapping
    public ApiResponse<BizfiFiVoucher> update(@RequestBody BizfiFiVoucher voucher) {
        return ApiResponse.success(service.updateDraft(voucher));
    }

    @PostMapping("/submit/{fid}")
    public ApiResponse<BizfiFiVoucher> submit(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.submit(fid));
    }

    @PostMapping("/audit/{fid}")
    public ApiResponse<BizfiFiVoucher> audit(@PathVariable("fid") Long fid,
                                             @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.audit(fid, operator));
    }

    @PostMapping("/post/{fid}")
    public ApiResponse<BizfiFiVoucher> post(@PathVariable("fid") Long fid,
                                            @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.post(fid, operator));
    }

    @PostMapping("/post/batch")
    public ApiResponse<Map<String, Object>> postBatch(@RequestBody List<Long> fids,
                                                      @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.postBatch(fids, operator));
    }

    @PostMapping("/reject/{fid}")
    public ApiResponse<BizfiFiVoucher> reject(@PathVariable("fid") Long fid,
                                              @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.reject(fid, operator));
    }

    @PostMapping("/reverse/{fid}")
    public ApiResponse<BizfiFiVoucher> reverse(@PathVariable("fid") Long fid,
                                               @RequestParam(value = "operator", required = false) String operator) {
        return ApiResponse.success(service.reverse(fid, operator));
    }

    @PostMapping("/import/ocr/parse")
    public ApiResponse<Map<String, Object>> ocrParse(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(voucherOcrService.parseFile(file));
    }

    @PostMapping("/import/ocr/confirm")
    public ApiResponse<BizfiFiVoucher> ocrConfirm(@RequestBody BizfiFiVoucherOcrImportRequest req) {
        if (req == null || req.getVoucher() == null) {
            throw new BizException("凭证数据不能为空");
        }
        BizfiFiVoucher saved = service.saveDraft(req.getVoucher());
        List<BizfiFiVoucherLine> lines = req.getLines();
        if (lines != null && !lines.isEmpty()) {
            service.saveLines(saved.getFid(), lines);
        }
        return ApiResponse.success(service.get(saved.getFid()));
    }

    @GetMapping("/{fid}")
    public ApiResponse<BizfiFiVoucher> detail(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.get(fid));
    }

    @GetMapping("/{fid}/source-docs")
    public ApiResponse<List<BizfiFiArapDoc>> sourceDocs(@PathVariable("fid") Long fid) {
        BizfiFiVoucher voucher = service.get(fid);
        if (voucher == null) {
            return ApiResponse.success(Collections.emptyList());
        }
        return ApiResponse.success(arapDocService.listByVoucher(fid, voucher.getFnumber()));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.deleteDraft(fid));
    }

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

    @GetMapping("/summary")
    public ApiResponse<VoucherSummaryResultVO> summary(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "summaryKeyword", required = false) String summaryKeyword
    ) {
        return ApiResponse.success(voucherAnalysisService.summary(startDate, endDate, status, summaryKeyword));
    }

    @GetMapping("/carry-list")
    public ApiResponse<VoucherCarryListResultVO> carryList(
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period
    ) {
        return ApiResponse.success(voucherAnalysisService.carryList(forg, period));
    }

    @GetMapping("/{fid}/lines")
    public ApiResponse<List<BizfiFiVoucherLine>> listLines(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.listLines(fid));
    }

    @PutMapping("/{fid}/lines")
    public ApiResponse<Boolean> saveLines(@PathVariable("fid") Long fid,
                                          @RequestBody List<BizfiFiVoucherLine> lines) {
        return ApiResponse.success(service.saveLines(fid, lines));
    }
}
