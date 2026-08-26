package single.cjj.erp.procurement.inbound.controller;

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
import single.cjj.erp.procurement.inbound.dto.PurchaseInboundContracts.PurchaseInboundCreateRequest;
import single.cjj.erp.procurement.inbound.dto.PurchaseInboundContracts.PurchaseInboundDetail;
import single.cjj.erp.procurement.inbound.dto.PurchaseInboundContracts.PurchaseInboundUpdateRequest;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntity;
import single.cjj.erp.procurement.inbound.service.PurchaseInboundService;

@RestController
@RequestMapping("/procurement/purchase-inbounds")
public class PurchaseInboundController {

    private final PurchaseInboundService service;

    public PurchaseInboundController(PurchaseInboundService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<PurchaseInboundDetail> create(
            @Valid @RequestBody PurchaseInboundCreateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.create(request, operatorId));
    }

    @PutMapping("/{fid}")
    public ApiResponse<PurchaseInboundDetail> update(
            @PathVariable("fid") Long fid,
            @Valid @RequestBody PurchaseInboundUpdateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.update(fid, request, operatorId));
    }

    @GetMapping("/{fid}")
    public ApiResponse<PurchaseInboundDetail> detail(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping
    public ApiResponse<IPage<PurchaseInboundEntity>> page(
            @RequestParam("tenantId") String tenantId,
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "number", required = false) String number,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "accountingStatus", required = false) String accountingStatus
    ) {
        return ApiResponse.success(service.page(tenantId, orgId, page, size, number, status, accountingStatus));
    }

    @PostMapping("/{fid}/submit")
    public ApiResponse<PurchaseInboundDetail> submit(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.submit(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/confirm")
    public ApiResponse<PurchaseInboundDetail> confirm(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.confirm(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/reject")
    public ApiResponse<PurchaseInboundDetail> reject(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.reject(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/cancel")
    public ApiResponse<PurchaseInboundDetail> cancel(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.cancel(fid, tenantId, operatorId));
    }
}
