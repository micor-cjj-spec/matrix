package single.cjj.fi.ap.payment;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.BotpCreateRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.BotpDocument;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.Detail;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.PayableSnapshot;

@RestController
@RequestMapping("/ap/internal/botp")
public class PaymentApplicationInternalController {

    private final PaymentApplicationService service;

    public PaymentApplicationInternalController(PaymentApplicationService service) {
        this.service = service;
    }

    @GetMapping("/payables/{fid}")
    public ApiResponse<BotpDocument> payable(@PathVariable Long fid) {
        return ApiResponse.success(service.botpPayable(fid));
    }

    @GetMapping("/payment-applications/{fid}")
    public ApiResponse<BotpDocument> application(@PathVariable Long fid) {
        return ApiResponse.success(service.botpApplication(fid));
    }

    @GetMapping("/payment-applications/by-idempotency")
    public ApiResponse<Detail> byIdempotency(
            @RequestParam String tenantId,
            @RequestParam String key
    ) {
        return ApiResponse.success(service.findByIdempotency(tenantId, key));
    }

    @PostMapping("/payment-applications")
    public ApiResponse<Detail> create(
            @Valid @RequestBody BotpCreateRequest request
    ) {
        return ApiResponse.success(service.createFromBotp(request));
    }

    @PostMapping("/payables/{fid}/recompute-reservation")
    public ApiResponse<PayableSnapshot> recompute(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestParam(required = false) Long operatorId
    ) {
        return ApiResponse.success(
                service.recomputePayableReservation(fid, tenantId, operatorId));
    }
}
