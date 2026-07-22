package single.cjj.openapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
}
