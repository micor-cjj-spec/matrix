package single.cjj.fi.integration.botp;

import java.time.LocalDateTime;

public final class BotpLifecycleContracts {

    private BotpLifecycleContracts() {
    }

    public record TargetStatusEvent(
            String eventId,
            String tenantId,
            String targetSystemCode,
            String targetDocumentType,
            String targetDocumentId,
            String targetStatus,
            String reason,
            String operator,
            LocalDateTime eventTime
    ) {
    }
}
