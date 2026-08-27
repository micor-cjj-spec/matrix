package single.cjj.erp.procurement.reverse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.erp.procurement.reverse.dto.PurchaseReverseContracts.*;
import single.cjj.erp.procurement.reverse.entity.PurchaseDeductionEntity;
import single.cjj.erp.procurement.reverse.entity.PurchaseReturnEntity;
import single.cjj.erp.procurement.reverse.entity.SupplierClaimEntity;
import single.cjj.erp.procurement.reverse.service.PurchaseDeductionService;
import single.cjj.erp.procurement.reverse.service.PurchaseReturnService;
import single.cjj.erp.procurement.reverse.service.SupplierClaimService;

@RestController
@RequestMapping("/procurement")
public class PurchaseReverseController {

    private final PurchaseReturnService returnService;
    private final SupplierClaimService claimService;
    private final PurchaseDeductionService deductionService;

    public PurchaseReverseController(
            PurchaseReturnService returnService,
            SupplierClaimService claimService,
            PurchaseDeductionService deductionService
    ) {
        this.returnService = returnService;
        this.claimService = claimService;
        this.deductionService = deductionService;
    }

    @PostMapping("/purchase-returns")
    public ApiResponse<ReturnDetail> createReturn(
            @Valid @RequestBody ReturnCreateRequest request,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(returnService.create(request, operatorId));
    }

    @GetMapping("/purchase-returns/{fid}")
    public ApiResponse<ReturnDetail> returnDetail(@PathVariable Long fid,@RequestParam String tenantId) {
        return ApiResponse.success(returnService.detail(fid, tenantId));
    }

    @GetMapping("/purchase-returns")
    public ApiResponse<IPage<PurchaseReturnEntity>> returnPage(
            @RequestParam String tenantId,
            @RequestParam(required=false) Long orgId,
            @RequestParam(required=false) Long purchaseOrderId,
            @RequestParam(required=false) Long purchaseInboundId,
            @RequestParam(required=false) String status,
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size) {
        return ApiResponse.success(returnService.page(
                tenantId,orgId,purchaseOrderId,purchaseInboundId,status,page,size));
    }

    @PostMapping("/purchase-returns/{fid}/submit")
    public ApiResponse<ReturnDetail> submitReturn(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(returnService.submit(fid, tenantId, operatorId));
    }

    @PostMapping("/purchase-returns/{fid}/confirm")
    public ApiResponse<ReturnDetail> confirmReturn(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(returnService.confirm(fid, tenantId, operatorId));
    }

    @PostMapping("/purchase-returns/{fid}/reject")
    public ApiResponse<ReturnDetail> rejectReturn(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(returnService.reject(fid, tenantId, operatorId));
    }

    @PostMapping("/purchase-returns/{fid}/cancel")
    public ApiResponse<ReturnDetail> cancelReturn(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(returnService.cancel(fid, tenantId, operatorId));
    }

    @PostMapping("/supplier-claims")
    public ApiResponse<ClaimDetail> createClaim(
            @Valid @RequestBody ClaimCreateRequest request,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(claimService.create(request, operatorId));
    }

    @GetMapping("/supplier-claims/{fid}")
    public ApiResponse<ClaimDetail> claimDetail(@PathVariable Long fid,@RequestParam String tenantId) {
        return ApiResponse.success(claimService.detail(fid, tenantId));
    }

    @GetMapping("/supplier-claims")
    public ApiResponse<IPage<SupplierClaimEntity>> claimPage(
            @RequestParam String tenantId,
            @RequestParam(required=false) Long orgId,
            @RequestParam(required=false) Long purchaseOrderId,
            @RequestParam(required=false) Long purchaseReturnId,
            @RequestParam(required=false) String status,
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size) {
        return ApiResponse.success(claimService.page(
                tenantId,orgId,purchaseOrderId,purchaseReturnId,status,page,size));
    }

    @PostMapping("/supplier-claims/{fid}/submit")
    public ApiResponse<ClaimDetail> submitClaim(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(claimService.submit(fid, tenantId, operatorId));
    }

    @PostMapping("/supplier-claims/{fid}/confirm")
    public ApiResponse<ClaimDetail> confirmClaim(@PathVariable Long fid,
            @Valid @RequestBody ClaimConfirmRequest request,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(claimService.confirm(fid, request, operatorId));
    }

    @PostMapping("/supplier-claims/{fid}/reject")
    public ApiResponse<ClaimDetail> rejectClaim(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(claimService.reject(fid, tenantId, operatorId));
    }

    @PostMapping("/supplier-claims/{fid}/cancel")
    public ApiResponse<ClaimDetail> cancelClaim(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(claimService.cancel(fid, tenantId, operatorId));
    }

    @PostMapping("/purchase-deductions")
    public ApiResponse<DeductionDetail> createDeduction(
            @Valid @RequestBody DeductionCreateRequest request,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(deductionService.create(request, operatorId));
    }

    @GetMapping("/purchase-deductions/{fid}")
    public ApiResponse<DeductionDetail> deductionDetail(@PathVariable Long fid,@RequestParam String tenantId) {
        return ApiResponse.success(deductionService.detail(fid, tenantId));
    }

    @GetMapping("/purchase-deductions")
    public ApiResponse<IPage<PurchaseDeductionEntity>> deductionPage(
            @RequestParam String tenantId,
            @RequestParam(required=false) Long orgId,
            @RequestParam(required=false) Long supplierClaimId,
            @RequestParam(required=false) String status,
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size) {
        return ApiResponse.success(deductionService.page(
                tenantId,orgId,supplierClaimId,status,page,size));
    }

    @PostMapping("/purchase-deductions/{fid}/submit")
    public ApiResponse<DeductionDetail> submitDeduction(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(deductionService.submit(fid, tenantId, operatorId));
    }

    @PostMapping("/purchase-deductions/{fid}/confirm")
    public ApiResponse<DeductionDetail> confirmDeduction(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(deductionService.confirm(fid, tenantId, operatorId));
    }

    @PostMapping("/purchase-deductions/{fid}/reject")
    public ApiResponse<DeductionDetail> rejectDeduction(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(deductionService.reject(fid, tenantId, operatorId));
    }

    @PostMapping("/purchase-deductions/{fid}/cancel")
    public ApiResponse<DeductionDetail> cancelDeduction(@PathVariable Long fid,@RequestParam String tenantId,
            @RequestHeader(value="X-Operator-Id", required=false) Long operatorId) {
        return ApiResponse.success(deductionService.cancel(fid, tenantId, operatorId));
    }
}
