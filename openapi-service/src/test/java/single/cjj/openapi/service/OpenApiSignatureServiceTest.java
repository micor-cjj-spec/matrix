package single.cjj.openapi.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSignatureServiceTest {

    private final OpenApiSignatureService service = new OpenApiSignatureService();

    @Test
    void shouldBuildStableCanonicalRequestAndSignature() {
        Map<String, String[]> query = new LinkedHashMap<>();
        query.put("status", new String[]{"POSTED"});
        query.put("pageSize", new String[]{"20"});
        query.put("pageNo", new String[]{"1"});

        String canonical = service.canonicalRequest(
                "GET",
                "/open-api/v1/fi/vouchers",
                query,
                new byte[0],
                "1784710000000",
                "nonce-1"
        );

        assertEquals(
                "GET\n" +
                        "/open-api/v1/fi/vouchers\n" +
                        "pageNo=1&pageSize=20&status=POSTED\n" +
                        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                        "1784710000000\n" +
                        "nonce-1",
                canonical
        );
        assertEquals(
                "e441c8d60646684ee042881718b6f8345dc72b3c235f9f73aecc0db753bcceb4",
                service.sign("secret", canonical)
        );
        assertTrue(service.verify(service.sign("secret", canonical), service.sign("secret", canonical)));
    }
}
