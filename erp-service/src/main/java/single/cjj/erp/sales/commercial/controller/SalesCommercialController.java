package single.cjj.erp.sales.commercial.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.erp.sales.commercial.dto.SalesCommercialContracts.*;
import single.cjj.erp.sales.commercial.entity.*;
import single.cjj.erp.sales.commercial.service.SalesCommercialService;
import single.cjj.erp.sales.commercial.security.SalesAccessGuard;
import single.cjj.erp.sales.commercial.security.SalesAccessGuard.Permission;

@RestController
@ConditionalOnProperty(prefix = "matrix.sales", name = "commercial-enabled", havingValue = "true", matchIfMissing = false)
@RequestMapping("/sales")
public class SalesCommercialController {
    private final SalesCommercialService service;
    private final SalesAccessGuard guard;

    public SalesCommercialController(SalesCommercialService service, SalesAccessGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @PostMapping("/quotes")
    public ApiResponse<QuoteDetail> createQuote(@RequestHeader(value = "Authorization", required = false) String bearer,
            @Valid @RequestBody CreateQuote request) {
        Long operator = guard.authorize(bearer, request.ftenantId(), request.forgId(), Permission.WRITE);
        return ApiResponse.success(service.createQuote(request, operator));
    }

    @GetMapping("/quotes")
    public ApiResponse<IPage<SalesQuoteEntity>> quotes(@RequestHeader(value = "Authorization", required = false) String bearer,
            @RequestParam String tenantId, @RequestParam Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        guard.authorize(bearer, tenantId, orgId, Permission.READ);
        return ApiResponse.success(service.quotePage(tenantId, orgId, status, page, size));
    }

    @GetMapping("/quotes/{id}")
    public ApiResponse<QuoteDetail> quote(@RequestHeader(value = "Authorization", required = false) String bearer,
            @PathVariable Long id, @RequestParam String tenantId) {
        guard.authorizeTenant(bearer, tenantId, Permission.READ);
        QuoteDetail detail = service.quoteDetail(id, tenantId);
        guard.authorize(bearer, tenantId, detail.header().getForgId(), Permission.READ);
        return ApiResponse.success(detail);
    }

    @PostMapping("/quotes/{id}/{action}")
    public ApiResponse<SalesQuoteEntity> changeQuote(@RequestHeader(value = "Authorization", required = false) String bearer,
            @PathVariable Long id, @PathVariable String action, @RequestParam String tenantId) {
        guard.authorizeTenant(bearer, tenantId, Permission.READ);
        SalesQuoteEntity existing = service.quoteDetail(id, tenantId).header();
        Permission required = switch (action) {
            case "submit", "send" -> Permission.WRITE;
            case "approve", "accept", "reject" -> Permission.APPROVE;
            default -> Permission.APPROVE;
        };
        Long operator = guard.authorize(bearer, tenantId, existing.getForgId(), required);
        return ApiResponse.success(service.transitionQuote(id, tenantId, action, operator));
    }

    @PostMapping("/contracts")
    public ApiResponse<ContractDetail> createContract(@RequestHeader(value = "Authorization", required = false) String bearer,
            @Valid @RequestBody CreateContract request) {
        guard.authorizeTenant(bearer, request.ftenantId(), Permission.WRITE);
        QuoteDetail source = service.quoteDetail(request.fquoteId(), request.ftenantId());
        Long operator = guard.authorize(bearer, request.ftenantId(), source.header().getForgId(), Permission.WRITE);
        return ApiResponse.success(service.createContract(request, operator));
    }

    @GetMapping("/contracts")
    public ApiResponse<IPage<SalesContractEntity>> contracts(@RequestHeader(value = "Authorization", required = false) String bearer,
            @RequestParam String tenantId, @RequestParam Long orgId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        guard.authorize(bearer, tenantId, orgId, Permission.READ);
        return ApiResponse.success(service.contractPage(tenantId, orgId, page, size));
    }

    @GetMapping("/contracts/{id}")
    public ApiResponse<ContractDetail> contract(@RequestHeader(value = "Authorization", required = false) String bearer,
            @PathVariable Long id, @RequestParam String tenantId) {
        guard.authorizeTenant(bearer, tenantId, Permission.READ);
        ContractDetail detail = service.contractDetail(id, tenantId);
        guard.authorize(bearer, tenantId, detail.header().getForgId(), Permission.READ);
        return ApiResponse.success(detail);
    }

    @PostMapping("/contracts/{id}/{action}")
    public ApiResponse<SalesContractEntity> changeContract(@RequestHeader(value = "Authorization", required = false) String bearer,
            @PathVariable Long id, @PathVariable String action, @RequestParam String tenantId) {
        guard.authorizeTenant(bearer, tenantId, Permission.READ);
        SalesContractEntity existing = service.contractDetail(id, tenantId).header();
        Permission required = "submit".equals(action) ? Permission.WRITE : Permission.APPROVE;
        Long operator = guard.authorize(bearer, tenantId, existing.getForgId(), required);
        return ApiResponse.success(service.transitionContract(id, tenantId, action, operator));
    }
}

