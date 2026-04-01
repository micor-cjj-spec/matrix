package single.cjj.bizfi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import single.cjj.bizfi.alert.AlertNotifier;
import single.cjj.bizfi.entity.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final AlertNotifier alertNotifier;

    public GlobalExceptionHandler(AlertNotifier alertNotifier) {
        this.alertNotifier = alertNotifier;
    }

    // 处理业务异常 BizException
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException ex) {
        log.warn("biz exception: {}", ex.getMessage());
        ApiResponse<Void> response = new ApiResponse<>(400, ex.getMessage(), null);
        // 返回 HTTP 400
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 处理其它未捕获异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("unexpected exception", ex);
        alertNotifier.notifyError("服务异常", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        ApiResponse<Void> response = new ApiResponse<>(500, "服务器错误", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
