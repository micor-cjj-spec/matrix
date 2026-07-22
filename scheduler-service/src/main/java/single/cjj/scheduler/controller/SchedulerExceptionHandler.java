package single.cjj.scheduler.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import single.cjj.bizfi.entity.ApiResponse;

@RestControllerAdvice(basePackages = "single.cjj.scheduler")
public class SchedulerExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ApiResponse<Void> handleBadRequest(Exception e) {
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().isEmpty()
                ? "参数校验失败"
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleUnreadable(HttpMessageNotReadableException e) {
        return ApiResponse.error(400, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpected(Exception e) {
        return ApiResponse.error(500, "调度服务处理失败: " + e.getMessage());
    }
}
