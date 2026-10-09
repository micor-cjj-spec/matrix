package single.cjj.fi.ai.tool;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class FinanceAiToolTokenGuard {

    public static final String HEADER_NAME = "X-Matrix-AI-Tool-Token";

    private final FinanceAiToolProperties properties;

    public FinanceAiToolTokenGuard(FinanceAiToolProperties properties) {
        this.properties = properties;
    }

    public void verify(String providedToken) {
        String configuredToken = properties.getInternalToken();
        if (!StringUtils.hasText(configuredToken)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "FINANCE_AI_TOOL_INTERNAL_TOKEN 未配置"
            );
        }
        if (!StringUtils.hasText(providedToken)
                || !MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "finance AI tool token 无效");
        }
    }
}
