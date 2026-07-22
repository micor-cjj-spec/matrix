package single.cjj.workflow.attachment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class WorkflowUploadSigner {

    private final byte[] secret;

    public WorkflowUploadSigner(
            @Value("${workflow.attachment.signing-secret:matrix-workflow-change-me}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String action, String fileId, long expiresEpochSecond) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                hmac(payload(action, fileId, expiresEpochSecond))
        );
    }

    public boolean verify(String action, String fileId, long expiresEpochSecond, String signature) {
        if (expiresEpochSecond < Instant.now().getEpochSecond() || signature == null) {
            return false;
        }
        byte[] expected = hmac(payload(action, fileId, expiresEpochSecond));
        try {
            byte[] actual = Base64.getUrlDecoder().decode(signature);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String payload(String action, String fileId, long expiresEpochSecond) {
        return action + ":" + fileId + ":" + expiresEpochSecond;
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成附件签名", ex);
        }
    }
}
