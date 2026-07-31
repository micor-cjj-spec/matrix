package single.cjj.bizfi.ai.audit;

import java.util.Set;

public record AiAuditOperatorContext(
        Long userId,
        Set<String> roles
) {
}
