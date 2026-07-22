package single.cjj.im.controller;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.im.service.ImBusinessException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ImExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ImExceptionHandler.class);

    @ExceptionHandler(ImBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusiness(ImBusinessException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::fieldErrorMessage)
                .collect(Collectors.joining("；"));
        return ApiResponse.error(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception e) {
        log.error("im-service unexpected error", e);
        return ApiResponse.error(500, "消息平台处理失败");
    }

    private String fieldErrorMessage(FieldError error) {
        return error.getField() + ": " + (error.getDefaultMessage() == null ? "参数错误" : error.getDefaultMessage());
    }
}
