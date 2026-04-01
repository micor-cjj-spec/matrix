package single.cjj.fi.gl.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.report.service.BizfiFiAnalysisReportService;
import single.cjj.fi.gl.report.vo.EnterpriseTaxResultVO;
import single.cjj.fi.gl.report.vo.FinancialIndicatorResultVO;

@RestController
@RequestMapping("/analysis-report")
public class BizfiFiAnalysisReportController {

    @Autowired
    private BizfiFiAnalysisReportService service;

    @GetMapping("/financial-indicators")
    public ApiResponse<FinancialIndicatorResultVO> financialIndicators(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "balanceTemplateId", required = false) Long balanceTemplateId,
            @RequestParam(value = "profitTemplateId", required = false) Long profitTemplateId
    ) {
        return ApiResponse.success(service.financialIndicators(orgId, period, currency, balanceTemplateId, profitTemplateId));
    }

    @GetMapping("/enterprise-tax")
    public ApiResponse<EnterpriseTaxResultVO> enterpriseTax(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "profitTemplateId", required = false) Long profitTemplateId
    ) {
        return ApiResponse.success(service.enterpriseTax(orgId, period, currency, profitTemplateId));
    }
}
