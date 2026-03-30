package single.cjj.fi.gl.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.report.service.BizfiFiCashFlowService;
import single.cjj.fi.gl.report.vo.CashFlowSupplementResultVO;
import single.cjj.fi.gl.report.vo.CashFlowTraceResultVO;
import single.cjj.fi.gl.report.vo.ReportQueryResultVO;

@RestController
@RequestMapping("/cash-flow")
public class BizfiFiCashFlowController {

    @Autowired
    private BizfiFiCashFlowService service;

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

    @GetMapping("/query")
    public ApiResponse<CashFlowTraceResultVO> trace(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "cashflowItemCode", required = false) String cashflowItemCode,
            @RequestParam(value = "categoryCode", required = false) String categoryCode,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(service.trace(orgId, period, currency, cashflowItemCode, categoryCode, sourceType, accountCode, keyword));
    }

    @GetMapping("/supplement")
    public ApiResponse<CashFlowSupplementResultVO> supplement(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "currency", required = false) String currency
    ) {
        return ApiResponse.success(service.supplement(orgId, period, currency));
    }
}
