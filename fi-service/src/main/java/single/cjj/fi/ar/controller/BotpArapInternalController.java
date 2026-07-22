package single.cjj.fi.ar.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.ar.dto.BotpArapContracts.ArapWritebackRequest;
import single.cjj.fi.ar.dto.BotpArapContracts.PaymentApplicationCreateRequest;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.service.BotpArapIntegrationService;

@RestController
@RequestMapping("/arap-doc/internal/botp")
public class BotpArapInternalController {

    private final BotpArapIntegrationService service;

    public BotpArapInternalController(BotpArapIntegrationService service) {
        this.service = service;
    }

    @GetMapping("/documents/{fid}")
    public ApiResponse<BizfiFiArapDoc> detail(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.detail(fid));
    }

    @GetMapping("/targets/by-idempotency")
    public ApiResponse<BizfiFiArapDoc> findByIdempotency(
            @RequestParam("key") String idempotencyKey
    ) {
        return ApiResponse.success(service.findByIdempotencyKey(idempotencyKey));
    }

    @PostMapping("/payment-applications")
    public ApiResponse<BizfiFiArapDoc> createPaymentApplication(
            @Valid @RequestBody PaymentApplicationCreateRequest request
    ) {
        return ApiResponse.success(service.createPaymentApplication(request));
    }

    @PostMapping("/documents/{fid}/writeback")
    public ApiResponse<BizfiFiArapDoc> recomputeWriteback(
            @PathVariable("fid") Long fid,
            @Valid @RequestBody ArapWritebackRequest request
    ) {
        return ApiResponse.success(service.recomputeWriteback(fid, request));
    }
}
