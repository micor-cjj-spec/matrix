package single.cjj.matrix.ai.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalTokenGuard {

    public static final String HEADER_NAME = "X-Matrix-Internal-Token";

    private final byte[] expectedToken;

    public InternalTokenGuard(MatrixAiProperties properties) {
        this.expectedToken = properties.getInternalToken().getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String suppliedToken) {
        byte[] supplied = suppliedToken == null
                ? new byte[0]
                : suppliedToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedToken, supplied)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "内部调用凭证无效");
        }
    }
}
