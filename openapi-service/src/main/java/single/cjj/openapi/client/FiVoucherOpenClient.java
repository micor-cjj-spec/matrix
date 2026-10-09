package single.cjj.openapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.openapi.contract.OpenApiPageResponse;
import single.cjj.openapi.contract.OpenVoucherLineResponse;
import single.cjj.openapi.contract.OpenVoucherResponse;

import java.util.List;

@FeignClient(
        name = "fi-service",
        contextId = "fiVoucherOpenClient",
        path = "/api/internal/openapi/v1/vouchers",
        configuration = FiOpenApiClientConfig.class
)
public interface FiVoucherOpenClient {

    @GetMapping
    ApiResponse<OpenApiPageResponse<OpenVoucherResponse>> list(
            @RequestParam("pageNo") int pageNo,
            @RequestParam("pageSize") int pageSize,
            @RequestParam(value = "voucherNumber", required = false) String voucherNumber,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "organizationId", required = false) String organizationId,
            @RequestParam(value = "bookId", required = false) String bookId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestHeader("X-OpenApi-Tenant-Id") String tenantId,
            @RequestHeader("X-OpenApi-Allowed-Statuses") String allowedStatuses,
            @RequestHeader("X-OpenApi-Allowed-Organizations") String allowedOrganizations,
            @RequestHeader("X-OpenApi-Allowed-Books") String allowedBooks
    );

    @GetMapping("/{voucherId}")
    ApiResponse<OpenVoucherResponse> detail(
            @PathVariable("voucherId") Long voucherId,
            @RequestHeader("X-OpenApi-Tenant-Id") String tenantId,
            @RequestHeader("X-OpenApi-Allowed-Statuses") String allowedStatuses,
            @RequestHeader("X-OpenApi-Allowed-Organizations") String allowedOrganizations,
            @RequestHeader("X-OpenApi-Allowed-Books") String allowedBooks
    );

    @GetMapping("/{voucherId}/lines")
    ApiResponse<List<OpenVoucherLineResponse>> lines(
            @PathVariable("voucherId") Long voucherId,
            @RequestHeader("X-OpenApi-Tenant-Id") String tenantId,
            @RequestHeader("X-OpenApi-Allowed-Statuses") String allowedStatuses,
            @RequestHeader("X-OpenApi-Allowed-Organizations") String allowedOrganizations,
            @RequestHeader("X-OpenApi-Allowed-Books") String allowedBooks
    );
}
