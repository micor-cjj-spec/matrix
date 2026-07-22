package single.cjj.openapi.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import single.cjj.openapi.controller.VoucherOpenApiController;
import single.cjj.openapi.dto.OpenApiEnvelope;
import single.cjj.openapi.security.OpenApiContext;

@RestControllerAdvice(assignableTypes = VoucherOpenApiController.class)
public class VoucherOpenApiExceptionHandler {

    @ExceptionHandler(OpenApiCallException.class)
    public ResponseEntity<OpenApiEnvelope<Void>> handleOpenApiCall(
            OpenApiCallException e,
            HttpServletRequest request) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(OpenApiEnvelope.error(e.getCode(), e.getMessage(), requestId(request)));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<OpenApiEnvelope<Void>> handleFeign(
            FeignException e,
            HttpServletRequest request) {
        return ResponseEntity.status(502)
                .body(OpenApiEnvelope.error(
                        "OPENAPI_50001",
                        "财务服务调用失败",
                        requestId(request)
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OpenApiEnvelope<Void>> handleUnknown(
            Exception e,
            HttpServletRequest request) {
        return ResponseEntity.status(500)
                .body(OpenApiEnvelope.error(
                        "OPENAPI_50001",
                        "OpenAPI内部服务异常",
                        requestId(request)
                ));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(OpenApiContext.REQUEST_ATTRIBUTE);
        return value instanceof OpenApiContext context ? context.getRequestId() : null;
    }
}
