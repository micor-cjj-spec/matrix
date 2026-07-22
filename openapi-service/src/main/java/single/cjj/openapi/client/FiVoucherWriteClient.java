package single.cjj.openapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.openapi.contract.OpenVoucherDraftCreateCommand;
import single.cjj.openapi.contract.OpenVoucherDraftCreateResult;

@FeignClient(
        name = "fi-service",
        contextId = "fiVoucherWriteClient",
        path = "/api/internal/openapi/v1/vouchers",
        configuration = FiOpenApiClientConfig.class
)
public interface FiVoucherWriteClient {

    @PostMapping("/drafts")
    ApiResponse<OpenVoucherDraftCreateResult> createDraft(
            @RequestBody OpenVoucherDraftCreateCommand command
    );

    @GetMapping("/by-source-request/{sourceRequestId}")
    ApiResponse<OpenVoucherDraftCreateResult> findBySourceRequest(
            @PathVariable("sourceRequestId") String sourceRequestId,
            @RequestHeader("X-OpenApi-Tenant-Id") String tenantId
    );
}
