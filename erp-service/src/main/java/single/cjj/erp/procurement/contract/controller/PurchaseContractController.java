package single.cjj.erp.procurement.contract.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.erp.procurement.contract.dto.PurchaseContractContracts.*;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntity;
import single.cjj.erp.procurement.contract.service.PurchaseContractService;

@RestController
@RequestMapping("/procurement/purchase-contracts")
public class PurchaseContractController {

    private final PurchaseContractService service;

    public PurchaseContractController(PurchaseContractService service) {
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
    public ApiResponse<IPage<PurchaseContractEntity>> page(
            @RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) Long businessPartnerId,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.page(
                tenantId, orgId, number, businessPartnerId, approvalStatus, status, page, size));
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

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.delete(fid, tenantId));
    }
}
