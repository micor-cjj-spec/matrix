package single.cjj.erp.procurement.acceptance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.erp.procurement.acceptance.dto.PurchaseAcceptanceContracts.PurchaseAcceptanceCreateRequest;
import single.cjj.erp.procurement.acceptance.dto.PurchaseAcceptanceContracts.PurchaseAcceptanceDetail;
import single.cjj.erp.procurement.acceptance.dto.PurchaseAcceptanceContracts.PurchaseAcceptanceUpdateRequest;
import single.cjj.erp.procurement.acceptance.entity.PurchaseAcceptanceEntity;
import single.cjj.erp.procurement.acceptance.service.PurchaseAcceptanceService;

@RestController
@RequestMapping("/procurement/purchase-acceptances")
public class PurchaseAcceptanceController {

    private final PurchaseAcceptanceService service;

    public PurchaseAcceptanceController(PurchaseAcceptanceService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<PurchaseAcceptanceDetail> create(
            @Valid @RequestBody PurchaseAcceptanceCreateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.create(request, operatorId));
    }

    @PutMapping("/{fid}")
    public ApiResponse<PurchaseAcceptanceDetail> update(
            @PathVariable("fid") Long fid,
            @Valid @RequestBody PurchaseAcceptanceUpdateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.update(fid, request, operatorId));
    }

    @GetMapping("/{fid}")
    public ApiResponse<PurchaseAcceptanceDetail> detail(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping
    public ApiResponse<IPage<PurchaseAcceptanceEntity>> page(
            @RequestParam("tenantId") String tenantId,
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "number", required = false) String number,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "result", required = false) String result
    ) {
        return ApiResponse.success(service.page(tenantId, orgId, page, size, number, status, result));
    }

    @PostMapping("/{fid}/submit")
    public ApiResponse<PurchaseAcceptanceDetail> submit(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.submit(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/confirm")
    public ApiResponse<PurchaseAcceptanceDetail> confirm(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.confirm(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/reject")
    public ApiResponse<PurchaseAcceptanceDetail> reject(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.reject(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/cancel")
    public ApiResponse<PurchaseAcceptanceDetail> cancel(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.cancel(fid, tenantId, operatorId));
    }
}
