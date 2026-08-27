package single.cjj.fi.fund.payment;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.fund.payment.PaymentOrderContracts.BotpCreateRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.BotpDocument;
import single.cjj.fi.fund.payment.PaymentOrderContracts.Detail;

@RestController
@RequestMapping("/fund/internal/botp")
public class PaymentOrderInternalController {

    private final PaymentOrderService service;

    public PaymentOrderInternalController(PaymentOrderService service) {
        this.service = service;
    }

    @GetMapping("/payment-applications/{fid}")
    public ApiResponse<BotpDocument> application(@PathVariable Long fid) {
        return ApiResponse.success(service.botpApplication(fid));
    }

    @GetMapping("/payment-orders/{fid}")
    public ApiResponse<BotpDocument> order(@PathVariable Long fid) {
        return ApiResponse.success(service.botpOrder(fid));
    }

    @GetMapping("/payment-orders/by-idempotency")
    public ApiResponse<Detail> byIdempotency(
            @RequestParam String tenantId,
            @RequestParam String key
    ) {
        return ApiResponse.success(service.findByIdempotency(tenantId, key));
    }

    @PostMapping("/payment-orders")
    public ApiResponse<Detail> create(@Valid @RequestBody BotpCreateRequest request) {
        return ApiResponse.success(service.createFromBotp(request));
    }

    @PostMapping("/payment-applications/{fid}/recompute-ordered")
    public ApiResponse<BotpDocument> recomputeApplicationOrdered(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestParam(required = false) Long operatorId
    ) {
        return ApiResponse.success(
                service.recomputeApplicationOrdered(fid, tenantId, operatorId));
    }
}
