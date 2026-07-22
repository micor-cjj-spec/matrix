package single.cjj.im.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Component
public class OpenApiSignatureFilter extends OncePerRequestFilter {

    public static final String APP_CODE_ATTRIBUTE = "im.openApi.appCode";

    private final OpenApiProperties properties;
    private final StringRedisTemplate redisTemplate;

    public OpenApiSignatureFilter(OpenApiProperties properties, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/open-api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CachedBodyRequest wrappedRequest = new CachedBodyRequest(request);
        String appCode;
        try {
            appCode = requiredHeader(request, "X-App-Code");
            String timestampText = requiredHeader(request, "X-Timestamp");
            String nonce = requiredHeader(request, "X-Nonce");
            String signature = requiredHeader(request, "X-Signature");

            String secret = properties.getCredentials().get(appCode);
            if (!StringUtils.hasText(secret)) {
                throw new SignatureException("未知或未启用的调用应用");
            }

            long timestamp = Long.parseLong(timestampText);
            long now = System.currentTimeMillis();
            long allowedMillis = properties.getSignatureWindowSeconds() * 1000L;
            if (Math.abs(now - timestamp) > allowedMillis) {
                throw new SignatureException("请求时间戳已过期");
            }

            String nonceKey = "im:open-api:nonce:" + appCode + ":" + nonce;
            Boolean accepted = redisTemplate.opsForValue().setIfAbsent(
                    nonceKey,
                    "1",
                    Duration.ofSeconds(properties.getSignatureWindowSeconds())
            );
            if (!Boolean.TRUE.equals(accepted)) {
                throw new SignatureException("检测到重复请求");
            }

            String bodyHash = sha256Hex(wrappedRequest.getCachedBody());
            String canonical = request.getMethod() + "\n"
                    + request.getRequestURI() + "\n"
                    + timestampText + "\n"
                    + nonce + "\n"
                    + bodyHash;
            String expected = hmacSha256Hex(secret, canonical);
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
                redisTemplate.delete(nonceKey);
                throw new SignatureException("签名校验失败");
            }
        } catch (NumberFormatException e) {
            writeUnauthorized(response, "时间戳格式错误");
            return;
        } catch (SignatureException e) {
            writeUnauthorized(response, e.getMessage());
            return;
        } catch (Exception e) {
            writeUnauthorized(response, "开放接口鉴权失败");
            return;
        }

        wrappedRequest.setAttribute(APP_CODE_ATTRIBUTE, appCode);
        filterChain.doFilter(wrappedRequest, response);
    }

    private String requiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (!StringUtils.hasText(value)) {
            throw new SignatureException("缺少请求头 " + name);
        }
        return value.trim();
    }

    private String sha256Hex(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private String hmacSha256Hex(String secret, String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":401,\"message\":\"" + escapeJson(message) + "\",\"data\":null}");
    }

    private String escapeJson(String value) {
        return value == null ? "鉴权失败" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class SignatureException extends RuntimeException {
        SignatureException(String message) {
            super(message);
        }
    }
}

@ConfigurationProperties(prefix = "im.open-api")
class OpenApiProperties {

    private int signatureWindowSeconds = 300;
    private Map<String, String> credentials = new HashMap<>();

    public int getSignatureWindowSeconds() {
        return signatureWindowSeconds;
    }

    public void setSignatureWindowSeconds(int signatureWindowSeconds) {
        this.signatureWindowSeconds = signatureWindowSeconds;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials == null ? new HashMap<>() : credentials;
    }
}

class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    CachedBodyRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    byte[] getCachedBody() {
        return cachedBody.clone();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // Synchronous request body wrapper; async callbacks are not required.
            }

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
