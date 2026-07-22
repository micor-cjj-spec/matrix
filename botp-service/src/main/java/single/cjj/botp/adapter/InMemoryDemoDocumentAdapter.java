package single.cjj.botp.adapter;

import org.springframework.stereotype.Component;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.WritebackCommand;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryDemoDocumentAdapter implements BotpDocumentAdapter {

    private static final String SYSTEM_CODE = "DEMO";
    private static final String SOURCE_TYPE = "DEMO_ORDER";
    private static final String TARGET_TYPE = "DEMO_DELIVERY";

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<String, TargetResult> targetByIdempotencyKey = new ConcurrentHashMap<>();
    private final Map<String, WritebackCommand> writebackByExecutionId = new ConcurrentHashMap<>();

    @Override
    public boolean supports(String systemCode, String documentType) {
        return SYSTEM_CODE.equals(systemCode)
                && (SOURCE_TYPE.equals(documentType) || TARGET_TYPE.equals(documentType));
    }

    @Override
    public DocumentData load(DocumentRef documentRef) {
        if (!SOURCE_TYPE.equals(documentRef.documentType())) {
            throw new BizException("演示适配器仅允许加载 DEMO_ORDER 源单");
        }

        List<Map<String, Object>> entries = new ArrayList<>();
        entries.add(Map.of(
                "entryId", "ENTRY-001",
                "materialId", "MATERIAL-001",
                "quantity", new BigDecimal("2"),
                "amount", new BigDecimal("199.00")
        ));
        entries.add(Map.of(
                "entryId", "ENTRY-002",
                "materialId", "MATERIAL-002",
                "quantity", new BigDecimal("1"),
                "amount", new BigDecimal("89.00")
        ));

        if (!documentRef.entryIds().isEmpty()) {
            entries.removeIf(entry -> !documentRef.entryIds().contains(String.valueOf(entry.get("entryId"))));
        }

        Map<String, Object> header = Map.of(
                "orderNo", documentRef.documentId(),
                "customerId", "CUSTOMER-001",
                "status", "APPROVED"
        );
        return new DocumentData(documentRef, header, entries);
    }

    @Override
    public void validateSource(DocumentData sourceDocument, Map<String, Object> context) {
        Object status = sourceDocument.header().get("status");
        if (!"APPROVED".equals(status)) {
            throw new BizException("演示订单未审核，不允许下推");
        }
        if (sourceDocument.entries().isEmpty()) {
            throw new BizException("演示订单没有可下推分录");
        }
    }

    @Override
    public Optional<TargetResult> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(targetByIdempotencyKey.get(idempotencyKey));
    }

    @Override
    public TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey) {
        if (!TARGET_TYPE.equals(targetDraft.documentType())) {
            throw new BizException("演示适配器仅允许创建 DEMO_DELIVERY 目标单");
        }
        return targetByIdempotencyKey.computeIfAbsent(idempotencyKey, key -> {
            long current = sequence.getAndIncrement();
            return new TargetResult(
                    SYSTEM_CODE,
                    TARGET_TYPE,
                    "DELIVERY-ID-" + current,
                    "DELIVERY-" + String.format("%06d", current)
            );
        });
    }

    @Override
    public void applyWriteback(WritebackCommand command) {
        writebackByExecutionId.putIfAbsent(command.executionId(), command);
    }
}
