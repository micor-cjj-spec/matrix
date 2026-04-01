package single.cjj.fi.gl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.service.BizfiFiLedgerQueryService;
import single.cjj.fi.gl.vo.LedgerQueryResultVO;

/**
 * 总账账表查询接口
 */
@RestController
@RequestMapping("/ledger")
public class BizfiFiLedgerQueryController {

    @Autowired
    private BizfiFiLedgerQueryService service;

    @GetMapping("/subject-balance")
    public ApiResponse<LedgerQueryResultVO> subjectBalance(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode
    ) {
        return ApiResponse.success(service.subjectBalance(startDate, endDate, accountCode));
    }

    @GetMapping("/general-ledger")
    public ApiResponse<LedgerQueryResultVO> generalLedger(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode
    ) {
        return ApiResponse.success(service.generalLedger(startDate, endDate, accountCode));
    }

    @GetMapping("/detail-ledger")
    public ApiResponse<LedgerQueryResultVO> detailLedger(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode
    ) {
        return ApiResponse.success(service.detailLedger(startDate, endDate, accountCode));
    }

    @GetMapping("/daily-report")
    public ApiResponse<LedgerQueryResultVO> dailyReport(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode
    ) {
        return ApiResponse.success(service.dailyReport(startDate, endDate, accountCode));
    }

    @GetMapping("/dimension-balance")
    public ApiResponse<LedgerQueryResultVO> dimensionBalance(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "dimensionCode", required = false) String dimensionCode
    ) {
        return ApiResponse.success(service.dimensionBalance(startDate, endDate, accountCode, dimensionCode));
    }

    @GetMapping("/aux-dimension-balance")
    public ApiResponse<LedgerQueryResultVO> auxDimensionBalance(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "dimensionCode", required = false) String dimensionCode
    ) {
        return ApiResponse.success(service.auxDimensionBalance(startDate, endDate, accountCode, dimensionCode));
    }

    @GetMapping("/aux-general-ledger")
    public ApiResponse<LedgerQueryResultVO> auxGeneralLedger(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "dimensionCode", required = false) String dimensionCode
    ) {
        return ApiResponse.success(service.auxGeneralLedger(startDate, endDate, accountCode, dimensionCode));
    }

    @GetMapping("/aux-detail-ledger")
    public ApiResponse<LedgerQueryResultVO> auxDetailLedger(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "dimensionCode", required = false) String dimensionCode
    ) {
        return ApiResponse.success(service.auxDetailLedger(startDate, endDate, accountCode, dimensionCode));
    }
}
