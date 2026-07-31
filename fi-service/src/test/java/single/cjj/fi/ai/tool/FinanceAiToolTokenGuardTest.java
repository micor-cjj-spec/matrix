package single.cjj.fi.ai.tool;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinanceAiToolTokenGuardTest {

    @Test
    void shouldVerifyConfiguredToken() {
        FinanceAiToolProperties properties = new FinanceAiToolProperties();
        properties.setInternalToken("secret");
        FinanceAiToolTokenGuard guard = new FinanceAiToolTokenGuard(properties);

        assertDoesNotThrow(() -> guard.verify("secret"));
        ResponseStatusException failure = assertThrows(
                ResponseStatusException.class,
                () -> guard.verify("wrong")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, failure.getStatusCode());
    }

    @Test
    void shouldFailClosedWhenTokenIsNotConfigured() {
        FinanceAiToolTokenGuard guard = new FinanceAiToolTokenGuard(new FinanceAiToolProperties());

        ResponseStatusException failure = assertThrows(
                ResponseStatusException.class,
                () -> guard.verify("anything")
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.getStatusCode());
    }
}
