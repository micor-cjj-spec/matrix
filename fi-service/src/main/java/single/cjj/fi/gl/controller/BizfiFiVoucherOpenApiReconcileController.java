package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.service.BizfiFiVoucherService;
import single.cjj.openapi.contract.OpenVoucherDraftCreateResult;

@RestController
@RequestMapping("/internal/openapi/v1/vouchers")
public class BizfiFiVoucherOpenApiReconcileController {

    private final BizfiFiVoucherService voucherService;

    public BizfiFiVoucherOpenApiReconcileController(BizfiFiVoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping("/by-source-request/{sourceRequestId}")
    public ApiResponse<OpenVoucherDraftCreateResult> findBySourceRequest(
            @PathVariable("sourceRequestId") String sourceRequestId,
            @RequestHeader("X-OpenApi-Tenant-Id") String tenantId) {
        BizfiFiVoucher voucher = voucherService.getOne(new LambdaQueryWrapper<BizfiFiVoucher>()
                .eq(BizfiFiVoucher::getTenantId, tenantId.trim())
                .eq(BizfiFiVoucher::getSourceRequestId, sourceRequestId.trim()), false);
        if (voucher == null) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(new OpenVoucherDraftCreateResult(
                voucher.getFid(), voucher.getFnumber(), voucher.getFstatus()
        ));
    }
}
