package single.cjj.botp.relation;

import org.junit.jupiter.api.Test;
import single.cjj.botp.adapter.BotpAdapterRegistry;
import single.cjj.botp.adapter.BotpDocumentAdapter;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.RelationStatus;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.RuleStatus;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.TargetStatusEvent;
import single.cjj.botp.domain.BotpContracts.WritebackCommand;
import single.cjj.botp.execution.InMemoryBotpExecutionLogRepository;
import single.cjj.botp.writeback.BotpWritebackService;
import single.cjj.botp.writeback.InMemoryBotpWritebackTaskRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotpRelationLifecycleServiceTest {

    @Test
    void shouldReverseOnceForDuplicateTargetVoidEvent() {
        InMemoryBotpRelationRepository relationRepository = new InMemoryBotpRelationRepository();
        RecordingSourceAdapter adapter = new RecordingSourceAdapter();
        InMemoryBotpExecutionLogRepository logs = new InMemoryBotpExecutionLogRepository();
        BotpWritebackService writebackService = new BotpWritebackService(
                new InMemoryBotpWritebackTaskRepository(),
                new BotpAdapterRegistry(List.of(adapter)),
                relationRepository,
                logs
        );
        BotpRelationLifecycleService lifecycleService = new BotpRelationLifecycleService(
                relationRepository, writebackService, logs);

        DocumentRef source = new DocumentRef("MATRIX", "FI_AP_DOC", "1001", List.of());
        TargetResult target = new TargetResult("MATRIX", "FI_PAYMENT_APPLICATION", "2001", "PAY-2001");
        RuleDefinition rule = new RuleDefinition(
                "AP_TO_PAYMENT_APPLICATION", "AP to Payment", 1, RuleStatus.PUBLISHED,
                "MATRIX", "FI_AP_DOC", "MATRIX", "FI_PAYMENT_APPLICATION",
                List.of(), List.of(), List.of()
        );
        var relation = relationRepository.saveActive(
                "default", "EXEC-1", rule, source, target, new BigDecimal("600"));

        TargetStatusEvent event = new TargetStatusEvent(
                "EVENT-VOID-2001", "default", "MATRIX", "FI_PAYMENT_APPLICATION", "2001",
                "VOID", "作废测试", "tester", LocalDateTime.now());
        lifecycleService.handleTargetStatusEvent(event);
        lifecycleService.handleTargetStatusEvent(event);

        assertEquals(1, adapter.writebackCount.get());
        assertEquals(BigDecimal.ZERO, adapter.lastActiveAmount);
        assertEquals(RelationStatus.REVERSED,
                relationRepository.findById(relation.relationId()).orElseThrow().status());
    }

    private static final class RecordingSourceAdapter implements BotpDocumentAdapter {
        private final AtomicInteger writebackCount = new AtomicInteger();
        private BigDecimal lastActiveAmount;

        @Override
        public boolean supports(String systemCode, String documentType) {
            return "MATRIX".equals(systemCode) && "FI_AP_DOC".equals(documentType);
        }

        @Override
        public DocumentData load(DocumentRef documentRef) {
            return new DocumentData(documentRef, java.util.Map.of(), List.of());
        }

        @Override
        public TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void applyWriteback(WritebackCommand command) {
            writebackCount.incrementAndGet();
            lastActiveAmount = (BigDecimal) command.context().get("activeAllocatedAmount");
        }
    }
}
