package single.cjj.erp.procurement.delivery.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.erp.procurement.delivery.dto.PurchaseDeliveryPlanContracts.*;
import single.cjj.erp.procurement.delivery.entity.PurchaseDeliveryPlanEntity;
import single.cjj.erp.procurement.delivery.service.PurchaseDeliveryPlanService;

import java.util.List;

@RestController
@RequestMapping("/procurement/delivery-plans")
public class PurchaseDeliveryPlanController {

    private final PurchaseDeliveryPlanService service;

    public PurchaseDeliveryPlanController(PurchaseDeliveryPlanService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<Detail> create(
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.create(request, operatorId));
    }

    @PutMapping("/{fid}")
    public ApiResponse<Detail> update(
            @PathVariable Long fid,
            @Valid @RequestBody UpdateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.update(fid, request, operatorId));
    }

    @GetMapping("/{fid}")
    public ApiResponse<Detail> detail(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping
    public ApiResponse<IPage<PurchaseDeliveryPlanEntity>> page(
            @RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long purchaseOrderId,
            @RequestParam(required = false) Long businessPartnerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.page(
                tenantId, orgId, purchaseOrderId, businessPartnerId, status, page, size));
    }

    @PostMapping("/{fid}/publish")
    public ApiResponse<Detail> publish(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.publish(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/supplier-responses")
    public ApiResponse<ResponseDetail> supplierResponse(
            @PathVariable Long fid,
            @Valid @RequestBody SupplierResponseRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.recordSupplierResponse(fid, request, operatorId));
    }

    @GetMapping("/{fid}/supplier-responses")
    public ApiResponse<List<ResponseDetail>> responses(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.listResponses(fid, tenantId));
    }

    @PostMapping("/{fid}/supplier-responses/{responseId}/accept")
    public ApiResponse<Detail> acceptResponse(
            @PathVariable Long fid,
            @PathVariable Long responseId,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(
                service.acceptChangeResponse(fid, responseId, tenantId, operatorId));
    }

    @PostMapping("/{fid}/supplier-responses/{responseId}/reject")
    public ApiResponse<Detail> rejectResponse(
            @PathVariable Long fid,
            @PathVariable Long responseId,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(
                service.rejectChangeResponse(fid, responseId, tenantId, operatorId));
    }

    @PostMapping("/{fid}/cancel")
    public ApiResponse<Detail> cancel(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.cancel(fid, tenantId, operatorId));
    }
}
