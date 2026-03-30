package single.cjj.fi.gl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.service.BizfiFiLedgerCollaborationService;

import java.util.Map;

@RestController
@RequestMapping("/ledger-collaboration")
public class BizfiFiLedgerCollaborationController {

    @Autowired
    private BizfiFiLedgerCollaborationService service;

    @GetMapping("/voucher-rules")
    public ApiResponse<Map<String, Object>> voucherRules(
            @RequestParam(value = "docTypeRoot", required = false) String docTypeRoot,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(service.voucherRules(docTypeRoot, keyword));
    }

    @GetMapping("/offset-vouchers")
    public ApiResponse<Map<String, Object>> offsetVouchers(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "matchStatus", required = false) String matchStatus,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(service.offsetVouchers(startDate, endDate, matchStatus, keyword));
    }

    @GetMapping("/voucher-check")
    public ApiResponse<Map<String, Object>> voucherCheck(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "issueCode", required = false) String issueCode,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "onlyIssue", required = false) Boolean onlyIssue
    ) {
        return ApiResponse.success(service.voucherChecks(startDate, endDate, issueCode, severity, status, onlyIssue));
    }

    @GetMapping("/subject-balance-compare")
    public ApiResponse<Map<String, Object>> subjectBalanceCompare(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "diffOnly", required = false) Boolean diffOnly
    ) {
        return ApiResponse.success(service.subjectBalanceCompare(startDate, endDate, accountCode, diffOnly));
    }
}
