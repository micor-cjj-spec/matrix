package single.cjj.im.service;

public class ImBusinessException extends RuntimeException {

    public ImBusinessException(String message) {
        super(message);
    }

    public ImBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
