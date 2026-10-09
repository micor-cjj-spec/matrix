package single.cjj.openapi.service;

import org.junit.jupiter.api.Test;
import single.cjj.openapi.exception.OpenApiCallException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenApiCallbackUrlValidatorTest {

    private final OpenApiCallbackUrlValidator validator = new OpenApiCallbackUrlValidator(false);

    @Test
    void shouldAllowEmptyWhenCallbackIsDisabled() {
        assertNull(validator.validateAndNormalize("  "));
    }

    @Test
    void shouldRejectHttpAndLocalAddresses() {
        OpenApiCallException http = assertThrows(
                OpenApiCallException.class,
                () -> validator.validateAndNormalize("http://example.com/callback")
        );
        assertEquals("OPENAPI_CALLBACK_40001", http.getCode());
        assertThrows(
                OpenApiCallException.class,
                () -> validator.validateAndNormalize("https://localhost/callback")
        );
        assertThrows(
                OpenApiCallException.class,
                () -> validator.validateAndNormalize("https://127.0.0.1/callback")
        );
    }
}
