package single.cjj.erp.procurement.invoice.controller;

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
import single.cjj.erp.procurement.invoice.dto.SupplierInvoiceContracts.SupplierInvoiceCreateRequest;
import single.cjj.erp.procurement.invoice.dto.SupplierInvoiceContracts.SupplierInvoiceDetail;
import single.cjj.erp.procurement.invoice.dto.SupplierInvoiceContracts.SupplierInvoiceUpdateRequest;
import single.cjj.erp.procurement.invoice.entity.SupplierInvoiceEntity;
import single.cjj.erp.procurement.invoice.service.SupplierInvoiceService;

@RestController
@RequestMapping("/procurement/supplier-invoices")
public class SupplierInvoiceController {

    private final SupplierInvoiceService service;

    public SupplierInvoiceController(SupplierInvoiceService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<SupplierInvoiceDetail> create(
            @Valid @RequestBody SupplierInvoiceCreateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.create(request, operatorId));
    }

    @PutMapping("/{fid}")
    public ApiResponse<SupplierInvoiceDetail> update(
            @PathVariable Long fid,
            @Valid @RequestBody SupplierInvoiceUpdateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.update(fid, request, operatorId));
    }

    @GetMapping("/{fid}")
    public ApiResponse<SupplierInvoiceDetail> detail(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping
    public ApiResponse<IPage<SupplierInvoiceEntity>> page(
            @RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String invoiceNo,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String matchStatus
    ) {
        return ApiResponse.success(service.page(tenantId, orgId, page, size, number, invoiceNo, approvalStatus, matchStatus));
    }

    @PostMapping("/{fid}/submit")
    public ApiResponse<SupplierInvoiceDetail> submit(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.submit(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/match")
    public ApiResponse<SupplierInvoiceDetail> match(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.match(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/audit")
    public ApiResponse<SupplierInvoiceDetail> audit(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.audit(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/reject")
    public ApiResponse<SupplierInvoiceDetail> reject(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.reject(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/cancel")
    public ApiResponse<SupplierInvoiceDetail> cancel(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.cancel(fid, tenantId, operatorId));
    }
}
