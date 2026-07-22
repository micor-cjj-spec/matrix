package single.cjj.openapi.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
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

    @Test
    void shouldIncludePostBodyHashInSignature() {
        byte[] body = "{\"externalBizNo\":\"EXP-001\",\"idempotencyKey\":\"expense:EXP-001\"}"
                .getBytes(StandardCharsets.UTF_8);
        String canonical = service.canonicalRequest(
                "POST",
                "/open-api/v1/fi/voucher-requests",
                Map.of(),
                body,
                "1784710000000",
                "nonce-write-1"
        );

        assertEquals(
                "POST\n" +
                        "/open-api/v1/fi/voucher-requests\n\n" +
                        "1de3b15095f7f6ad6f901a82eac829870bf182f0f4ae1a1de891847d3fe5db0b\n" +
                        "1784710000000\n" +
                        "nonce-write-1",
                canonical
        );
        assertEquals(
                "3f270d30a8f6820d725a23d89de0e94d30ae3b024d14a304855c8274e4925cac",
                service.sign("secret", canonical)
        );
    }
}
