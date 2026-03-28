package single.cjj.fi.gl.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.report.service.BizfiFiProfitStatementService;
import single.cjj.fi.gl.report.vo.ReportQueryResultVO;

@RestController
@RequestMapping("/profit-statement")
public class BizfiFiProfitStatementController {

    @Autowired
    private BizfiFiProfitStatementService service;

    @GetMapping
    public ApiResponse<ReportQueryResultVO> query(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "startPeriod", required = false) String startPeriod,
            @RequestParam(value = "endPeriod", required = false) String endPeriod,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "showZero", required = false, defaultValue = "true") Boolean showZero
    ) {
        return ApiResponse.success(service.query(orgId, startPeriod, endPeriod, currency, templateId, showZero));
    }
}
