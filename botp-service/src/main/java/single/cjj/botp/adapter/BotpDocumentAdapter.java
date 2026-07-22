package single.cjj.botp.adapter;

import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.WritebackCommand;

import java.util.Map;
import java.util.Optional;

public interface BotpDocumentAdapter {

    boolean supports(String systemCode, String documentType);

    DocumentData load(DocumentRef documentRef);

    default void validateSource(DocumentData sourceDocument, Map<String, Object> context) {
    }

    default Optional<TargetResult> findByIdempotencyKey(String idempotencyKey) {
        return Optional.empty();
    }

    TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey);

    default void applyWriteback(WritebackCommand command) {
    }
}
