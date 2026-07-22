package single.cjj.openapi.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiCallbackSignatureServiceTest {

    private final OpenApiCallbackSignatureService service =
            new OpenApiCallbackSignatureService(new OpenApiSignatureService());

    @Test
    void shouldBuildStableCallbackSignature() {
        String body = "{\"requestId\":\"req_1\",\"status\":\"SUCCEEDED\"}";
        String canonical = service.canonical(
                "cb_event_1", "1784710000000", "nonce-1", body.getBytes()
        );
        assertEquals(
                "cb_event_1\n1784710000000\nnonce-1\n" +
                        "d0c4360ad112a6319c720932b7ca12ced56de8ab470780488ca531be70273a23",
                canonical
        );
        assertEquals(
                "23c2f39361f248f39882d1462578c585163966a496e5c27845c309449ff4eb51",
                service.sign("secret", "cb_event_1", "1784710000000", "nonce-1", body)
        );
    }
}
