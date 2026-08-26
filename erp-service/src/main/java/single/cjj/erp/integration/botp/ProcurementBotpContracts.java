package single.cjj.erp.integration.botp;

import java.util.List;
import java.util.Map;

public final class ProcurementBotpContracts {

    private ProcurementBotpContracts() {
    }

    public record BotpDocumentResponse(
            String systemCode,
            String documentType,
            String documentId,
            String documentNo,
            Map<String, Object> header,
            List<Map<String, Object>> entries
    ) {
    }

    public record BotpTargetCreateRequest(
            String idempotencyKey,
            Map<String, Object> header,
            List<Map<String, Object>> entries
    ) {
    }

    public record BotpTargetEntryResult(
            String correlationKey,
            String targetEntryId
    ) {
    }

    public record BotpTargetResponse(
            String systemCode,
            String documentType,
            String documentId,
            String documentNo,
            List<BotpTargetEntryResult> entries
    ) {
    }
}
