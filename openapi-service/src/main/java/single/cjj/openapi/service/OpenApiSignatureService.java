package single.cjj.openapi.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class OpenApiSignatureService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public String canonicalRequest(String method,
                                   String path,
                                   Map<String, String[]> queryParameters,
                                   byte[] requestBody,
                                   String timestamp,
                                   String nonce) {
        return String.join("\n",
                method == null ? "" : method.toUpperCase(),
                path == null ? "" : path,
                canonicalQuery(queryParameters),
                sha256Hex(requestBody == null ? new byte[0] : requestBody),
                timestamp == null ? "" : timestamp,
                nonce == null ? "" : nonce
        );
    }

    public String sign(String appSecret, String canonicalRequest) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return hex(mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("OpenAPI 签名计算失败", e);
        }
    }

    public boolean verify(String expectedSignature, String actualSignature) {
        if (expectedSignature == null || actualSignature == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedSignature.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actualSignature.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String canonicalQuery(Map<String, String[]> queryParameters) {
        if (queryParameters == null || queryParameters.isEmpty()) {
            return "";
        }
        List<QueryPart> parts = new ArrayList<>();
        queryParameters.forEach((key, values) -> {
            String[] safeValues = values == null || values.length == 0 ? new String[]{""} : values;
            Arrays.stream(safeValues)
                    .sorted()
                    .forEach(value -> parts.add(new QueryPart(encode(key), encode(value))));
        });
        parts.sort(Comparator.comparing(QueryPart::key).thenComparing(QueryPart::value));
        return parts.stream()
                .map(part -> part.key() + "=" + part.value())
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    public String sha256Hex(byte[] value) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private record QueryPart(String key, String value) {
    }
}
