package single.cjj.fi.ar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.ar.service.BizfiFiArapManageService;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/arap-manage")
public class BizfiFiArapManageController {

    @Autowired
    private BizfiFiArapManageService service;

    @GetMapping("/plan")
    public ApiResponse<Map<String, Object>> plan(
            @RequestParam(value = "docTypeRoot", defaultValue = "AR") String docTypeRoot,
            @RequestParam(value = "counterparty", required = false) String counterparty,
            @RequestParam(value = "asOfDate", required = false) String asOfDate,
            @RequestParam(value = "auditedOnly", required = false, defaultValue = "false") Boolean auditedOnly
    ) {
        return ApiResponse.success(service.plan(docTypeRoot, counterparty, parseDate(asOfDate), auditedOnly));
    }

    @PostMapping("/auto-writeoff")
    public ApiResponse<Map<String, Object>> autoWriteoff(
            @RequestParam(value = "docTypeRoot", defaultValue = "AR") String docTypeRoot,
            @RequestParam(value = "counterparty", required = false) String counterparty,
            @RequestParam(value = "asOfDate", required = false) String asOfDate,
            @RequestParam(value = "operator", required = false) String operator
    ) {
        return ApiResponse.success(service.autoWriteoff(docTypeRoot, counterparty, parseDate(asOfDate), operator));
    }

    @GetMapping("/statement")
    public ApiResponse<Map<String, Object>> statement(
            @RequestParam(value = "docTypeRoot", defaultValue = "AR") String docTypeRoot,
            @RequestParam(value = "counterparty", required = false) String counterparty,
            @RequestParam(value = "asOfDate", required = false) String asOfDate,
            @RequestParam(value = "openOnly", required = false, defaultValue = "false") Boolean openOnly
    ) {
        return ApiResponse.success(service.statement(docTypeRoot, counterparty, parseDate(asOfDate), openOnly));
    }

    @GetMapping("/account-query")
    public ApiResponse<Map<String, Object>> accountQuery(
            @RequestParam(value = "docTypeRoot", defaultValue = "AR") String docTypeRoot,
            @RequestParam(value = "counterparty", required = false) String counterparty,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "openOnly", required = false, defaultValue = "false") Boolean openOnly,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "asOfDate", required = false) String asOfDate
    ) {
        return ApiResponse.success(service.accountQuery(docTypeRoot, counterparty, docType, status, openOnly, keyword, parseDate(asOfDate)));
    }

    @GetMapping("/writeoff-log")
    public ApiResponse<Map<String, Object>> writeoffLog(
            @RequestParam(value = "docTypeRoot", defaultValue = "AR") String docTypeRoot,
            @RequestParam(value = "counterparty", required = false) String counterparty,
            @RequestParam(value = "planCode", required = false) String planCode,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return ApiResponse.success(service.writeoffLog(docTypeRoot, counterparty, planCode, parseDate(startDate), parseDate(endDate)));
    }

    @GetMapping("/aging-analysis")
    public ApiResponse<Map<String, Object>> agingAnalysis(
            @RequestParam(value = "docTypeRoot", defaultValue = "AR") String docTypeRoot,
            @RequestParam(value = "asOfDate", required = false) String asOfDate
    ) {
        return ApiResponse.success(service.agingAnalysis(docTypeRoot, parseDate(asOfDate)));
    }

    @GetMapping("/multi-analysis")
    public ApiResponse<Map<String, Object>> multiAnalysis(
            @RequestParam(value = "docTypeRoot", defaultValue = "AR") String docTypeRoot,
            @RequestParam(value = "groupDimension", required = false, defaultValue = "COUNTERPARTY") String groupDimension,
            @RequestParam(value = "asOfDate", required = false) String asOfDate
    ) {
        return ApiResponse.success(service.multiAnalysis(docTypeRoot, groupDimension, parseDate(asOfDate)));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LocalDate.now();
        }
        return LocalDate.parse(value.trim());
    }
}
