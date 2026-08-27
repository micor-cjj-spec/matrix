package single.cjj.botp.integration.fi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.BotpCreateRequest;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.BotpDocument;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.PayableSnapshot;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.PaymentApplicationDetail;

@FeignClient(
        name = "fi-payment-application-client",
        url = "${botp.fi-service-url:}",
        path = "/api/ap/internal/botp"
)
public interface FiPaymentApplicationClient {

    @GetMapping("/payables/{fid}")
    ApiResponse<BotpDocument> payable(@PathVariable("fid") Long fid);

    @GetMapping("/payment-applications/{fid}")
    ApiResponse<BotpDocument> application(@PathVariable("fid") Long fid);

    @GetMapping("/payment-applications/by-idempotency")
    ApiResponse<PaymentApplicationDetail> findByIdempotency(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("key") String key
    );

    @PostMapping("/payment-applications")
    ApiResponse<PaymentApplicationDetail> create(
            @RequestBody BotpCreateRequest request
    );

    @PostMapping("/payables/{fid}/recompute-reservation")
    ApiResponse<PayableSnapshot> recomputeReservation(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId,
            @RequestParam(value = "operatorId", required = false) Long operatorId
    );
}
