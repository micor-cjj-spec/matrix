package single.cjj.fi.ap.settlement;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.ap.settlement.PaymentSettlementContracts.Detail;
import single.cjj.fi.ap.settlement.PaymentSettlementContracts.FinalizeRequest;

@RestController
@RequestMapping("/fund/payment-settlements")
public class PaymentSettlementController {

    private final PaymentSettlementService service;

    public PaymentSettlementController(PaymentSettlementService service) {
        this.service = service;
    }

    @PostMapping("/payment-orders/{paymentOrderId}/finalize")
    public ApiResponse<Detail> finalizePayment(
            @PathVariable Long paymentOrderId,
            @RequestParam String tenantId,
            @RequestBody(required = false) FinalizeRequest request
    ) {
        Long operatorId = request == null ? null : request.operatorId();
        return ApiResponse.success(
                service.finalizePayment(paymentOrderId, tenantId, operatorId));
    }

    @GetMapping("/{settlementId}")
    public ApiResponse<Detail> detail(
            @PathVariable Long settlementId,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.detail(settlementId, tenantId));
    }
}
