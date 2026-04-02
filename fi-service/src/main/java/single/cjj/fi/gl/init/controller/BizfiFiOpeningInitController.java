package single.cjj.fi.gl.init.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.gl.init.entity.BizfiFiCashflowOpening;
import single.cjj.fi.gl.init.entity.BizfiFiCounterpartyOpeningBalance;
import single.cjj.fi.gl.init.entity.BizfiFiSubjectOpeningBalance;
import single.cjj.fi.gl.init.service.BizfiFiCashflowOpeningService;
import single.cjj.fi.gl.init.service.BizfiFiCounterpartyOpeningBalanceService;
import single.cjj.fi.gl.init.service.BizfiFiSubjectOpeningBalanceService;

import java.util.HashMap;
import java.util.Map;

@RestController
public class BizfiFiOpeningInitController {

    @Autowired
    private BizfiFiSubjectOpeningBalanceService subjectService;

    @Autowired
    private BizfiFiCashflowOpeningService cashflowService;

    @Autowired
    private BizfiFiCounterpartyOpeningBalanceService counterpartyService;

    @GetMapping("/opening-subject/{fid}")
    public ApiResponse<BizfiFiSubjectOpeningBalance> getSubject(@PathVariable("fid") Long fid) {
        return ApiResponse.success(subjectService.get(fid));
    }

    @PostMapping("/opening-subject")
    public ApiResponse<BizfiFiSubjectOpeningBalance> addSubject(@RequestBody BizfiFiSubjectOpeningBalance item) {
        return ApiResponse.success(subjectService.add(item));
    }

    @PutMapping("/opening-subject")
    public ApiResponse<BizfiFiSubjectOpeningBalance> updateSubject(@RequestBody BizfiFiSubjectOpeningBalance item) {
        return ApiResponse.success(subjectService.update(item));
    }

    @DeleteMapping("/opening-subject/{fid}")
    public ApiResponse<Boolean> deleteSubject(@PathVariable("fid") Long fid) {
        return ApiResponse.success(subjectService.delete(fid));
    }

    @GetMapping("/opening-subject/list")
    public ApiResponse<IPage<BizfiFiSubjectOpeningBalance>> listSubject(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "fperiod", required = false) String fperiod,
            @RequestParam(value = "faccountCode", required = false) String faccountCode,
            @RequestParam(value = "faccountName", required = false) String faccountName
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("forg", forg);
        query.put("fperiod", fperiod);
        query.put("faccountCode", faccountCode);
        query.put("faccountName", faccountName);
        return ApiResponse.success(subjectService.list(page, size, query));
    }

    @GetMapping("/opening-cashflow/{fid}")
    public ApiResponse<BizfiFiCashflowOpening> getCashflow(@PathVariable("fid") Long fid) {
        return ApiResponse.success(cashflowService.get(fid));
    }

    @PostMapping("/opening-cashflow")
    public ApiResponse<BizfiFiCashflowOpening> addCashflow(@RequestBody BizfiFiCashflowOpening item) {
        return ApiResponse.success(cashflowService.add(item));
    }

    @PutMapping("/opening-cashflow")
    public ApiResponse<BizfiFiCashflowOpening> updateCashflow(@RequestBody BizfiFiCashflowOpening item) {
        return ApiResponse.success(cashflowService.update(item));
    }

    @DeleteMapping("/opening-cashflow/{fid}")
    public ApiResponse<Boolean> deleteCashflow(@PathVariable("fid") Long fid) {
        return ApiResponse.success(cashflowService.delete(fid));
    }

    @GetMapping("/opening-cashflow/list")
    public ApiResponse<IPage<BizfiFiCashflowOpening>> listCashflow(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "fperiod", required = false) String fperiod,
            @RequestParam(value = "fcashflowCode", required = false) String fcashflowCode,
            @RequestParam(value = "fcashflowName", required = false) String fcashflowName
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("forg", forg);
        query.put("fperiod", fperiod);
        query.put("fcashflowCode", fcashflowCode);
        query.put("fcashflowName", fcashflowName);
        return ApiResponse.success(cashflowService.list(page, size, query));
    }

    @GetMapping("/opening-counterparty/{fid}")
    public ApiResponse<BizfiFiCounterpartyOpeningBalance> getCounterparty(@PathVariable("fid") Long fid) {
        return ApiResponse.success(counterpartyService.get(fid));
    }

    @PostMapping("/opening-counterparty")
    public ApiResponse<BizfiFiCounterpartyOpeningBalance> addCounterparty(@RequestBody BizfiFiCounterpartyOpeningBalance item) {
        return ApiResponse.success(counterpartyService.add(item));
    }

    @PutMapping("/opening-counterparty")
    public ApiResponse<BizfiFiCounterpartyOpeningBalance> updateCounterparty(@RequestBody BizfiFiCounterpartyOpeningBalance item) {
        return ApiResponse.success(counterpartyService.update(item));
    }

    @DeleteMapping("/opening-counterparty/{fid}")
    public ApiResponse<Boolean> deleteCounterparty(@PathVariable("fid") Long fid) {
        return ApiResponse.success(counterpartyService.delete(fid));
    }

    @GetMapping("/opening-counterparty/list")
    public ApiResponse<IPage<BizfiFiCounterpartyOpeningBalance>> listCounterparty(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "forg", required = false) Long forg,
            @RequestParam(value = "fperiod", required = false) String fperiod,
            @RequestParam(value = "fcounterpartyType", required = false) String fcounterpartyType,
            @RequestParam(value = "fcounterpartyName", required = false) String fcounterpartyName
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("forg", forg);
        query.put("fperiod", fperiod);
        query.put("fcounterpartyType", fcounterpartyType);
        query.put("fcounterpartyName", fcounterpartyName);
        return ApiResponse.success(counterpartyService.list(page, size, query));
    }
}
