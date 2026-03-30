package single.cjj.fi.notice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.notice.service.BizfiFiInternalNoticeService;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/internal-notice")
public class BizfiFiInternalNoticeController {

    @Autowired
    private BizfiFiInternalNoticeService service;

    @GetMapping("/counterparty")
    public ApiResponse<Map<String, Object>> queryCounterparty(
            @RequestParam(value = "docTypeRoot", defaultValue = "AR") String docTypeRoot,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "asOfDate", required = false) String asOfDate
    ) {
        return ApiResponse.success(service.queryCounterpartyNotices(docTypeRoot, status, severity, parseDate(asOfDate)));
    }

    @PostMapping("/counterparty/generate")
    public ApiResponse<Map<String, Object>> generateCounterparty(
            @RequestParam(value = "docTypeRoot", defaultValue = "AR") String docTypeRoot,
            @RequestParam(value = "asOfDate", required = false) String asOfDate,
            @RequestParam(value = "operator", required = false) String operator
    ) {
        return ApiResponse.success(service.generateCounterpartyNotices(docTypeRoot, parseDate(asOfDate), operator));
    }

    @GetMapping("/counterparty/reconcile")
    public ApiResponse<Map<String, Object>> reconcileCounterparty(
            @RequestParam(value = "docTypeRoot", defaultValue = "AR") String docTypeRoot,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "asOfDate", required = false) String asOfDate
    ) {
        return ApiResponse.success(service.reconcileCounterpartyNotices(docTypeRoot, status, parseDate(asOfDate)));
    }

    @GetMapping("/cashflow")
    public ApiResponse<Map<String, Object>> queryCashflow(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "sourceCode", required = false) String sourceCode,
            @RequestParam(value = "currency", required = false) String currency
    ) {
        return ApiResponse.success(service.queryCashflowNotices(orgId, period, status, severity, sourceCode, currency));
    }

    @PostMapping("/cashflow/generate")
    public ApiResponse<Map<String, Object>> generateCashflow(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "operator", required = false) String operator
    ) {
        return ApiResponse.success(service.generateCashflowNotices(orgId, period, currency, operator));
    }

    @GetMapping("/cashflow/reconcile")
    public ApiResponse<Map<String, Object>> reconcileCashflow(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "currency", required = false) String currency
    ) {
        return ApiResponse.success(service.reconcileCashflowNotices(orgId, period, status, currency));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LocalDate.now();
        }
        return LocalDate.parse(value.trim());
    }
}
