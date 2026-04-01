package single.cjj.fi.gl.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.report.service.BizfiFiBalanceSheetService;
import single.cjj.fi.gl.report.vo.BalanceSheetDrillResultVO;
import single.cjj.fi.gl.report.vo.ReportQueryResultVO;

@RestController
@RequestMapping("/balance-sheet")
public class BizfiFiBalanceSheetController {

    @Autowired
    private BizfiFiBalanceSheetService service;

    @GetMapping
    public ApiResponse<ReportQueryResultVO> query(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "showZero", required = false, defaultValue = "true") Boolean showZero
    ) {
        return ApiResponse.success(service.query(orgId, period, currency, templateId, showZero));
    }

    @GetMapping("/drill")
    public ApiResponse<BalanceSheetDrillResultVO> drill(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "itemCode", required = false) String itemCode
    ) {
        return ApiResponse.success(service.drill(orgId, period, currency, templateId, itemId, itemCode));
    }
}
