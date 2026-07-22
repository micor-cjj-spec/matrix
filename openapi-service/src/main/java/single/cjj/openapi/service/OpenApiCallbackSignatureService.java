package single.cjj.openapi.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class OpenApiCallbackSignatureService {

    private final OpenApiSignatureService signatureService;

    public OpenApiCallbackSignatureService(OpenApiSignatureService signatureService) {
        this.signatureService = signatureService;
    }

    public String canonical(String eventId, String timestamp, String nonce, byte[] body) {
        return String.join("\n",
                eventId == null ? "" : eventId,
                timestamp == null ? "" : timestamp,
                nonce == null ? "" : nonce,
                signatureService.sha256Hex(body == null ? new byte[0] : body));
    }

    public String sign(String appSecret, String eventId, String timestamp, String nonce, String body) {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        return signatureService.sign(appSecret, canonical(eventId, timestamp, nonce, bytes));
    }
}
