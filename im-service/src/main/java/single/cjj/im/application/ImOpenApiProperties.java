package single.cjj.im.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "im.open-api")
public class ImOpenApiProperties {

    private int signatureWindowSeconds = 300;
    private int fallbackRateLimitPerMinute = 600;
    private Map<String, String> credentials = new HashMap<>();

    public int getSignatureWindowSeconds() {
        return signatureWindowSeconds;
    }

    public void setSignatureWindowSeconds(int signatureWindowSeconds) {
        this.signatureWindowSeconds = Math.max(30, signatureWindowSeconds);
    }

    public int getFallbackRateLimitPerMinute() {
        return fallbackRateLimitPerMinute;
    }

    public void setFallbackRateLimitPerMinute(int fallbackRateLimitPerMinute) {
        this.fallbackRateLimitPerMinute = Math.max(1, fallbackRateLimitPerMinute);
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials == null ? new HashMap<>() : credentials;
    }
}
