package single.cjj.erp.procurement.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderCreateRequest;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderDetail;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderFromContractCreateRequest;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderUpdateRequest;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.service.PurchaseOrderContractConversionService;
import single.cjj.erp.procurement.order.service.PurchaseOrderService;

@RestController
@RequestMapping("/procurement/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService service;
    private final PurchaseOrderContractConversionService contractConversionService;

    public PurchaseOrderController(
            PurchaseOrderService service,
            PurchaseOrderContractConversionService contractConversionService
    ) {
        this.service = service;
        this.contractConversionService = contractConversionService;
    }

    @PostMapping
    public ApiResponse<PurchaseOrderDetail> create(
            @Valid @RequestBody PurchaseOrderCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.create(request, operatorId));
    }

    @PostMapping("/from-contract")
    public ApiResponse<PurchaseOrderDetail> createFromContract(
            @Valid @RequestBody PurchaseOrderFromContractCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(
                contractConversionService.createFromContract(request, operatorId));
    }

    @PutMapping("/{fid}")
    public ApiResponse<PurchaseOrderDetail> update(
            @PathVariable Long fid,
            @Valid @RequestBody PurchaseOrderUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.update(fid, request, operatorId));
    }

    @GetMapping("/{fid}")
    public ApiResponse<PurchaseOrderDetail> detail(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping
    public ApiResponse<IPage<PurchaseOrderEntity>> page(
            @RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) Long businessPartnerId,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(service.page(
                tenantId,
                orgId,
                page,
                size,
                number,
                businessPartnerId,
                approvalStatus,
                status
        ));
    }

    @PostMapping("/{fid}/submit")
    public ApiResponse<PurchaseOrderDetail> submit(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.submit(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/audit")
    public ApiResponse<PurchaseOrderDetail> audit(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.audit(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/reject")
    public ApiResponse<PurchaseOrderDetail> reject(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.reject(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/cancel")
    public ApiResponse<PurchaseOrderDetail> cancel(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.cancel(fid, tenantId, operatorId));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.delete(fid, tenantId));
    }
}