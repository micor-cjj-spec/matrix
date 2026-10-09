package single.cjj.openapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.openapi.client.FiVoucherOpenClient;
import single.cjj.openapi.contract.OpenApiPageResponse;
import single.cjj.openapi.contract.OpenVoucherLineResponse;
import single.cjj.openapi.contract.OpenVoucherQuery;
import single.cjj.openapi.contract.OpenVoucherResponse;
import single.cjj.openapi.dto.OpenApiEnvelope;
import single.cjj.openapi.exception.OpenApiCallException;
import single.cjj.openapi.security.OpenApiContext;
import single.cjj.openapi.service.OpenApiPermissionService;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/open-api/v1/fi/vouchers")
public class VoucherOpenApiController {

    private final FiVoucherOpenClient voucherClient;
    private final OpenApiPermissionService permissionService;
    private final int systemMaxPageSize;

    public VoucherOpenApiController(FiVoucherOpenClient voucherClient,
                                    OpenApiPermissionService permissionService,
                                    @Value("${matrix.openapi.system-max-page-size:500}") int systemMaxPageSize) {
        this.voucherClient = voucherClient;
        this.permissionService = permissionService;
        this.systemMaxPageSize = Math.max(1, systemMaxPageSize);
    }

    @GetMapping
    public OpenApiEnvelope<OpenApiPageResponse<OpenVoucherResponse>> list(
            @ModelAttribute OpenVoucherQuery query,
            HttpServletRequest request) {
        OpenApiContext context = context(request);
        OpenApiPermissionService.VoucherPermission permission =
                permissionService.resolveVoucherPermission(context.getApp(), context.getGrant());

        String status = resolveStatus(query.getStatus(), permission.allowedStatuses());
        String organizationId = resolveScope(
                query.getOrganizationId(), permission.organizationIds(), "组织"
        );
        String bookId = resolveScope(query.getBookId(), permission.bookIds(), "账簿");

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.minusMonths(permission.maxHistoryMonths());
        LocalDate startDate = query.getStartDate() == null || query.getStartDate().isBefore(cutoff)
                ? cutoff
                : query.getStartDate();
        LocalDate endDate = query.getEndDate() == null || query.getEndDate().isAfter(today)
                ? today
                : query.getEndDate();
        if (startDate.isAfter(endDate)) {
            throw new OpenApiCallException("OPENAPI_40001", "startDate不能晚于endDate", 400);
        }

        int pageNo = query.getPageNo() == null ? 1 : Math.max(query.getPageNo(), 1);
        int requestedPageSize = query.getPageSize() == null ? 20 : Math.max(query.getPageSize(), 1);
        int pageSize = Math.min(requestedPageSize, resolvePageSizeLimit(context));

        ApiResponse<OpenApiPageResponse<OpenVoucherResponse>> response = voucherClient.list(
                pageNo,
                pageSize,
                query.getVoucherNumber(),
                status,
                organizationId,
                bookId,
                startDate.toString(),
                endDate.toString(),
                permission.tenantId(),
                scopeHeader(permission.allowedStatuses()),
                scopeHeader(permission.organizationIds()),
                scopeHeader(permission.bookIds())
        );
        return OpenApiEnvelope.success(context.getRequestId(), unwrap(response));
    }

    @GetMapping("/{voucherId}")
    public OpenApiEnvelope<OpenVoucherResponse> detail(
            @PathVariable("voucherId") Long voucherId,
            HttpServletRequest request) {
        OpenApiContext context = context(request);
        OpenApiPermissionService.VoucherPermission permission =
                permissionService.resolveVoucherPermission(context.getApp(), context.getGrant());
        OpenVoucherResponse voucher = loadAndValidateVoucher(voucherId, permission);
        return OpenApiEnvelope.success(context.getRequestId(), voucher);
    }

    @GetMapping("/{voucherId}/lines")
    public OpenApiEnvelope<List<OpenVoucherLineResponse>> lines(
            @PathVariable("voucherId") Long voucherId,
            HttpServletRequest request) {
        OpenApiContext context = context(request);
        OpenApiPermissionService.VoucherPermission permission =
                permissionService.resolveVoucherPermission(context.getApp(), context.getGrant());
        loadAndValidateVoucher(voucherId, permission);
        ApiResponse<List<OpenVoucherLineResponse>> response = voucherClient.lines(
                voucherId,
                permission.tenantId(),
                scopeHeader(permission.allowedStatuses()),
                scopeHeader(permission.organizationIds()),
                scopeHeader(permission.bookIds())
        );
        return OpenApiEnvelope.success(context.getRequestId(), unwrap(response));
    }

    private OpenVoucherResponse loadAndValidateVoucher(
            Long voucherId,
            OpenApiPermissionService.VoucherPermission permission) {
        ApiResponse<OpenVoucherResponse> response = voucherClient.detail(
                voucherId,
                permission.tenantId(),
                scopeHeader(permission.allowedStatuses()),
                scopeHeader(permission.organizationIds()),
                scopeHeader(permission.bookIds())
        );
        OpenVoucherResponse voucher = unwrap(response);
        if (voucher == null) {
            throw new OpenApiCallException("OPENAPI_40401", "凭证不存在", 404);
        }
        LocalDate cutoff = LocalDate.now().minusMonths(permission.maxHistoryMonths());
        if (voucher.getVoucherDate() != null && voucher.getVoucherDate().isBefore(cutoff)) {
            throw new OpenApiCallException("OPENAPI_40303", "凭证超出授权历史范围", 403);
        }
        return voucher;
    }

    private String resolveStatus(String requestedStatus, Set<String> allowedStatuses) {
        if (StringUtils.hasText(requestedStatus)) {
            String status = requestedStatus.trim().toUpperCase();
            if (!allowedStatuses.contains(status)) {
                throw new OpenApiCallException("OPENAPI_40303", "请求的凭证状态不在授权范围内", 403);
            }
            return status;
        }
        return allowedStatuses.iterator().next();
    }

    private String resolveScope(String requestedValue, Set<String> allowedValues, String label) {
        if (!StringUtils.hasText(requestedValue)) {
            return null;
        }
        String normalized = requestedValue.trim();
        if (!allowedValues.contains("*") && !allowedValues.contains(normalized)) {
            throw new OpenApiCallException(
                    "OPENAPI_40303", "请求的" + label + "不在授权范围内", 403
            );
        }
        return normalized;
    }

    private int resolvePageSizeLimit(OpenApiContext context) {
        int appLimit = context.getApp().getMaxPageSize() == null || context.getApp().getMaxPageSize() <= 0
                ? 200
                : context.getApp().getMaxPageSize();
        int apiLimit = context.getDefinition().getMaxPageSize() == null
                || context.getDefinition().getMaxPageSize() <= 0
                ? systemMaxPageSize
                : context.getDefinition().getMaxPageSize();
        return Math.max(1, Math.min(systemMaxPageSize, Math.min(appLimit, apiLimit)));
    }

    private String scopeHeader(Set<String> values) {
        return values.stream().sorted().collect(Collectors.joining(","));
    }

    private OpenApiContext context(HttpServletRequest request) {
        Object value = request.getAttribute(OpenApiContext.REQUEST_ATTRIBUTE);
        if (!(value instanceof OpenApiContext context)) {
            throw new OpenApiCallException("OPENAPI_50001", "OpenAPI认证上下文缺失", 500);
        }
        return context;
    }

    private <T> T unwrap(ApiResponse<T> response) {
        if (response == null) {
            throw new OpenApiCallException("OPENAPI_50001", "财务服务无响应", 502);
        }
        if (response.getCode() != 200) {
            throw new OpenApiCallException(
                    "OPENAPI_50001",
                    StringUtils.hasText(response.getMessage()) ? response.getMessage() : "财务服务调用失败",
                    502
            );
        }
        return response.getData();
    }
}
