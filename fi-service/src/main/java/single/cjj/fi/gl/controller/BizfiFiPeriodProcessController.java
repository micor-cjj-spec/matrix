package single.cjj.fi.gl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.service.BizfiFiPeriodProcessService;
import single.cjj.fi.gl.vo.PeriodMonitorCenterResultVO;
import single.cjj.fi.gl.vo.PeriodProcessResultVO;

@RestController
@RequestMapping("/period-process")
public class BizfiFiPeriodProcessController {

    @Autowired
    private BizfiFiPeriodProcessService service;

    @GetMapping("/profit-loss")
    public ApiResponse<PeriodProcessResultVO> profitLoss(
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period
    ) {
        return ApiResponse.success(service.profitLoss(forg, period));
    }

    @GetMapping("/auto-transfer")
    public ApiResponse<PeriodProcessResultVO> autoTransfer(
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period
    ) {
        return ApiResponse.success(service.autoTransfer(forg, period));
    }

    @GetMapping("/fx-revalue")
    public ApiResponse<PeriodProcessResultVO> fxRevalue(
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period
    ) {
        return ApiResponse.success(service.fxRevalue(forg, period));
    }

    @GetMapping("/voucher-amortization")
    public ApiResponse<PeriodProcessResultVO> voucherAmortization(
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period
    ) {
        return ApiResponse.success(service.voucherAmortization(forg, period));
    }

    @GetMapping("/close-books")
    public ApiResponse<PeriodProcessResultVO> closeBooks(
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period
    ) {
        return ApiResponse.success(service.closeBooks(forg, period));
    }

    @GetMapping("/monitor-center")
    public ApiResponse<PeriodMonitorCenterResultVO> monitorCenter(
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "period", required = false) String period
    ) {
        return ApiResponse.success(service.monitorCenter(forg, period));
    }
}
