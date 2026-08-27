package single.cjj.erp.procurement.request.controller;

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
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.ApprovalResultRequest;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.CreateRequest;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.Detail;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.UpdateRequest;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntity;
import single.cjj.erp.procurement.request.service.PurchaseRequestService;

@RestController
@RequestMapping("/procurement/purchase-requests")
public class PurchaseRequestController {

    private final PurchaseRequestService service;

    public PurchaseRequestController(PurchaseRequestService service) {
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
    public ApiResponse<IPage<PurchaseRequestEntity>> page(
            @RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(service.page(
                tenantId, orgId, page, size, number, requesterId, approvalStatus, status));
    }

    @PostMapping("/{fid}/submit")
    public ApiResponse<Detail> submit(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.submit(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/approval-result")
    public ApiResponse<Detail> approvalResult(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @Valid @RequestBody ApprovalResultRequest request
    ) {
        return ApiResponse.success(service.applyApprovalResult(fid, tenantId, request));
    }

    @PostMapping("/{fid}/cancel")
    public ApiResponse<Detail> cancel(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
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
