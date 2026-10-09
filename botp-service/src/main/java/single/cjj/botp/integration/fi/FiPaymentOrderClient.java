package single.cjj.botp.integration.fi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.botp.integration.fi.FiPaymentOrderClientContracts.BotpCreateRequest;
import single.cjj.botp.integration.fi.FiPaymentOrderClientContracts.BotpDocument;
import single.cjj.botp.integration.fi.FiPaymentOrderClientContracts.PaymentOrderDetail;

@FeignClient(
        name = "fi-payment-order-client",
        url = "${botp.fi-service-url:}",
        path = "/api/fund/internal/botp"
)
public interface FiPaymentOrderClient {

    @GetMapping("/payment-applications/{fid}")
    ApiResponse<BotpDocument> application(@PathVariable("fid") Long fid);

    @GetMapping("/payment-orders/{fid}")
    ApiResponse<BotpDocument> order(@PathVariable("fid") Long fid);

    @GetMapping("/payment-orders/by-idempotency")
    ApiResponse<PaymentOrderDetail> findByIdempotency(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("key") String key
    );

    @PostMapping("/payment-orders")
    ApiResponse<PaymentOrderDetail> create(@RequestBody BotpCreateRequest request);

    @PostMapping("/payment-applications/{fid}/recompute-ordered")
    ApiResponse<BotpDocument> recomputeApplicationOrdered(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestParam(value = "operatorId", required = false) Long operatorId
    );
}
