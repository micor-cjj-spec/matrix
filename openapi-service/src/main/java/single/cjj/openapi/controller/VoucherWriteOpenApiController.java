package single.cjj.openapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.openapi.dto.OpenApiEnvelope;
import single.cjj.openapi.dto.VoucherWriteCreateRequest;
import single.cjj.openapi.dto.VoucherWriteStatusResponse;
import single.cjj.openapi.exception.OpenApiCallException;
import single.cjj.openapi.security.OpenApiContext;
import single.cjj.openapi.service.OpenApiVoucherWriteService;

@RestController
@RequestMapping("/open-api/v1/fi/voucher-requests")
public class VoucherWriteOpenApiController {

    private final OpenApiVoucherWriteService writeService;

    public VoucherWriteOpenApiController(OpenApiVoucherWriteService writeService) {
        this.writeService = writeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OpenApiEnvelope<VoucherWriteStatusResponse> create(
            @RequestBody VoucherWriteCreateRequest input,
            HttpServletRequest request) {
        OpenApiContext context = context(request);
        Object bodyHash = request.getAttribute(OpenApiContext.REQUEST_BODY_HASH_ATTRIBUTE);
        VoucherWriteStatusResponse result = writeService.accept(
                context,
                input,
                bodyHash == null ? null : String.valueOf(bodyHash)
        );
        return OpenApiEnvelope.success(context.getRequestId(), result);
    }

    @GetMapping("/{requestId}")
    public OpenApiEnvelope<VoucherWriteStatusResponse> status(
            @PathVariable("requestId") String requestId,
            HttpServletRequest request) {
        OpenApiContext context = context(request);
        VoucherWriteStatusResponse result = writeService.statusForApp(
                context.getApp().getId(), requestId
        );
        return OpenApiEnvelope.success(context.getRequestId(), result);
    }

    @GetMapping("/by-external-no/{externalBizNo}")
    public OpenApiEnvelope<VoucherWriteStatusResponse> statusByExternalNo(
            @PathVariable("externalBizNo") String externalBizNo,
            HttpServletRequest request) {
        OpenApiContext context = context(request);
        VoucherWriteStatusResponse result = writeService.statusForAppByExternalNo(
                context.getApp().getId(), externalBizNo
        );
        return OpenApiEnvelope.success(context.getRequestId(), result);
    }

    private OpenApiContext context(HttpServletRequest request) {
        Object value = request.getAttribute(OpenApiContext.REQUEST_ATTRIBUTE);
        if (!(value instanceof OpenApiContext context)) {
            throw new OpenApiCallException("OPENAPI_50001", "OpenAPI认证上下文缺失", 500);
        }
        return context;
    }
}
