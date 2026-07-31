package single.cjj.fi.ai.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinanceAiToolTokenGuardTest {

    @Test
    void shouldVerifyConfiguredToken() {
        FinanceAiToolProperties properties = new FinanceAiToolProperties();
        properties.setInternalToken("secret");
        FinanceAiToolTokenGuard guard = new FinanceAiToolTokenGuard(properties);

        assertDoesNotThrow(() -> guard.verify("secret"));
        assertThrows(SecurityException.class, () -> guard.verify("wrong"));
    }

    @Test
    void shouldFailClosedWhenTokenIsNotConfigured() {
        FinanceAiToolTokenGuard guard = new FinanceAiToolTokenGuard(new FinanceAiToolProperties());

        assertThrows(IllegalStateException.class, () -> guard.verify("anything"));
    }
}
