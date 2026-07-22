package single.cjj.openapi.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.openapi.controller.OpenApiAdminController;

@RestControllerAdvice(assignableTypes = OpenApiAdminController.class)
public class OpenApiAdminExceptionHandler {

    @ExceptionHandler(OpenApiCallException.class)
    public ResponseEntity<ApiResponse<Void>> handleOpenApiCall(OpenApiCallException e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getHttpStatus(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "开放平台内部服务异常"));
    }
}
