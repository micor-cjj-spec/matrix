package single.cjj.share.exception;

import java.util.HashMap;
import java.util.Map;

public class ValidationException extends RuntimeException {
    private final Map<String, String> errors = new HashMap<>();

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException addError(String field, String message) {
        this.errors.put(field, message);
        return this;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
