package single.cjj.matrix.ai.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalTokenGuardTest {

    @Test
    void shouldAcceptMatchingTokenAndRejectOthers() {
        MatrixAiProperties properties = new MatrixAiProperties();
        properties.setInternalToken("secret-token");
        InternalTokenGuard guard = new InternalTokenGuard(properties);

        assertDoesNotThrow(() -> guard.verify("secret-token"));
        assertThrows(ResponseStatusException.class, () -> guard.verify("wrong-token"));
        assertThrows(ResponseStatusException.class, () -> guard.verify(null));
    }
}
