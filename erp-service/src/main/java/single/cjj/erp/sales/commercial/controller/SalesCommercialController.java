package single.cjj.erp.sales.commercial.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.erp.sales.commercial.dto.SalesCommercialContracts.*;
import single.cjj.erp.sales.commercial.entity.*;
import single.cjj.erp.sales.commercial.service.SalesCommercialService;

@RestController
@RequestMapping("/sales")
public class SalesCommercialController {
    private final SalesCommercialService service;

    public SalesCommercialController(SalesCommercialService service) { this.service = service; }

    @PostMapping("/quotes")
    public ApiResponse<QuoteDetail> createQuote(@Valid @RequestBody CreateQuote request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operator) {
        return ApiResponse.success(service.createQuote(request, operator));
    }

    @GetMapping("/quotes")
    public ApiResponse<IPage<SalesQuoteEntity>> quotes(@RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.quotePage(tenantId, orgId, status, page, size));
    }

    @GetMapping("/quotes/{id}")
    public ApiResponse<QuoteDetail> quote(@PathVariable Long id, @RequestParam String tenantId) {
        return ApiResponse.success(service.quoteDetail(id, tenantId));
    }

    @PostMapping("/quotes/{id}/{action}")
    public ApiResponse<SalesQuoteEntity> changeQuote(@PathVariable Long id, @PathVariable String action,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operator) {
        return ApiResponse.success(service.transitionQuote(id, tenantId, action, operator));
    }

    @PostMapping("/contracts")
    public ApiResponse<ContractDetail> createContract(@Valid @RequestBody CreateContract request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operator) {
        return ApiResponse.success(service.createContract(request, operator));
    }

    @GetMapping("/contracts")
    public ApiResponse<IPage<SalesContractEntity>> contracts(@RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.contractPage(tenantId, orgId, page, size));
    }

    @GetMapping("/contracts/{id}")
    public ApiResponse<ContractDetail> contract(@PathVariable Long id, @RequestParam String tenantId) {
        return ApiResponse.success(service.contractDetail(id, tenantId));
    }

    @PostMapping("/contracts/{id}/{action}")
    public ApiResponse<SalesContractEntity> changeContract(@PathVariable Long id, @PathVariable String action,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operator) {
        return ApiResponse.success(service.transitionContract(id, tenantId, action, operator));
    }
}

