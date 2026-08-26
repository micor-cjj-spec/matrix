package single.cjj.erp.procurement.receipt.controller;

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
import single.cjj.erp.procurement.receipt.dto.PurchaseReceiptContracts.PurchaseReceiptCreateRequest;
import single.cjj.erp.procurement.receipt.dto.PurchaseReceiptContracts.PurchaseReceiptDetail;
import single.cjj.erp.procurement.receipt.dto.PurchaseReceiptContracts.PurchaseReceiptUpdateRequest;
import single.cjj.erp.procurement.receipt.entity.PurchaseReceiptEntity;
import single.cjj.erp.procurement.receipt.service.PurchaseReceiptService;

@RestController
@RequestMapping("/procurement/purchase-receipts")
public class PurchaseReceiptController {

    private final PurchaseReceiptService service;

    public PurchaseReceiptController(PurchaseReceiptService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<PurchaseReceiptDetail> create(
            @Valid @RequestBody PurchaseReceiptCreateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.create(request, operatorId));
    }

    @PutMapping("/{fid}")
    public ApiResponse<PurchaseReceiptDetail> update(
            @PathVariable("fid") Long fid,
            @Valid @RequestBody PurchaseReceiptUpdateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.update(fid, request, operatorId));
    }

    @GetMapping("/{fid}")
    public ApiResponse<PurchaseReceiptDetail> detail(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping
    public ApiResponse<IPage<PurchaseReceiptEntity>> page(
            @RequestParam("tenantId") String tenantId,
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "number", required = false) String number,
            @RequestParam(value = "businessPartnerId", required = false) Long businessPartnerId,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(service.page(tenantId, orgId, page, size, number, businessPartnerId, status));
    }

    @PostMapping("/{fid}/submit")
    public ApiResponse<PurchaseReceiptDetail> submit(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.submit(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/confirm")
    public ApiResponse<PurchaseReceiptDetail> confirm(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.confirm(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/reject")
    public ApiResponse<PurchaseReceiptDetail> reject(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.reject(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/cancel")
    public ApiResponse<PurchaseReceiptDetail> cancel(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.cancel(fid, tenantId, operatorId));
    }
}
