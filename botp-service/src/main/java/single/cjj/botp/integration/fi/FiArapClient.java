package single.cjj.botp.integration.fi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.botp.integration.fi.FiArapClientContracts.ArapWritebackRequest;
import single.cjj.botp.integration.fi.FiArapClientContracts.FiArapDocument;
import single.cjj.botp.integration.fi.FiArapClientContracts.PaymentApplicationCreateRequest;

@FeignClient(
        name = "fi-service",
        url = "${botp.fi-service-url:}",
        path = "/api/arap-doc/internal/botp"
)
public interface FiArapClient {

    @GetMapping("/documents/{fid}")
    ApiResponse<FiArapDocument> detail(@PathVariable("fid") Long fid);

    @GetMapping("/targets/by-idempotency")
    ApiResponse<FiArapDocument> findByIdempotency(@RequestParam("key") String idempotencyKey);

    @PostMapping("/payment-applications")
    ApiResponse<FiArapDocument> createPaymentApplication(
            @RequestBody PaymentApplicationCreateRequest request
    );

    @PostMapping("/documents/{fid}/writeback")
    ApiResponse<FiArapDocument> recomputeWriteback(
            @PathVariable("fid") Long fid,
            @RequestBody ArapWritebackRequest request
    );
}
