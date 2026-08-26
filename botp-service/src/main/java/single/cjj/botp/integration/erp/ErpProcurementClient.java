package single.cjj.botp.integration.erp;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.botp.integration.erp.ErpProcurementClientContracts.BotpDocumentResponse;
import single.cjj.botp.integration.erp.ErpProcurementClientContracts.BotpTargetCreateRequest;
import single.cjj.botp.integration.erp.ErpProcurementClientContracts.BotpTargetResponse;

@FeignClient(
        name = "erp-service",
        url = "${botp.erp-service-url:}",
        path = "/api/procurement/internal/botp"
)
public interface ErpProcurementClient {

    @GetMapping("/documents/{documentType}/{fid}")
    ApiResponse<BotpDocumentResponse> document(
            @PathVariable("documentType") String documentType,
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId
    );

    @GetMapping("/targets/{documentType}/by-idempotency")
    ApiResponse<BotpTargetResponse> findByIdempotency(
            @PathVariable("documentType") String documentType,
            @RequestParam("key") String key
    );

    @PostMapping("/targets/{documentType}")
    ApiResponse<BotpTargetResponse> createTarget(
            @PathVariable("documentType") String documentType,
            @RequestBody BotpTargetCreateRequest request
    );
}
