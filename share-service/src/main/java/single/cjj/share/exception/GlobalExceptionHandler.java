package single.cjj.share.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("资源不存在");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        detail.setTitle("参数校验失败");
        detail.setDetail(ex.getMessage());
        detail.setProperty("errors", ex.getErrors());
        return detail;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ProblemDetail handleBeanValidation(Exception ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        detail.setTitle("参数校验失败");
        Map<String, String> errors = new HashMap<>();
        if (ex instanceof MethodArgumentNotValidException manve) {
            manve.getBindingResult().getFieldErrors()
                    .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        } else if (ex instanceof BindException bindException) {
            bindException.getBindingResult().getFieldErrors()
                    .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        }
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        detail.setTitle("系统异常");
        detail.setDetail(ex.getMessage());
        return detail;
    }
}
