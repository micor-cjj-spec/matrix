package single.cjj.fi.expense.workflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import single.cjj.bizfi.exception.BizException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class ExpenseWorkflowCallbackSigner {

    private final byte[] secret;
    private final long maximumSkewSeconds;

    public ExpenseWorkflowCallbackSigner(
            @Value("${fi.workflow.callback-secret:change-me-in-production}") String secret,
            @Value("${fi.workflow.callback-max-skew-seconds:300}") long maximumSkewSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.maximumSkewSeconds = maximumSkewSeconds;
    }

    public void verify(String timestampValue, String signatureValue, String body) {
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampValue);
        } catch (Exception ex) {
            throw new BizException("工作流回调时间戳无效");
        }
        if (Math.abs(Instant.now().getEpochSecond() - timestamp) > maximumSkewSeconds) {
            throw new BizException("工作流回调已过期");
        }
        String expected = "sha256=" + hmac(timestamp + "." + body);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] suppliedBytes = signatureValue == null
                ? new byte[0] : signatureValue.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, suppliedBytes)) {
            throw new BizException("工作流回调签名校验失败");
        }
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("无法计算工作流回调签名", ex);
        }
    }
}
