package single.cjj.fi.ai.tool;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
            throw new IllegalStateException("FINANCE_AI_TOOL_INTERNAL_TOKEN 未配置");
        }
        if (!StringUtils.hasText(providedToken)
                || !MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new SecurityException("finance AI tool token 无效");
        }
    }
}
