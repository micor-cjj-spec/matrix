package single.cjj.botp.writeback;

import org.junit.jupiter.api.Test;
import single.cjj.botp.adapter.BotpAdapterRegistry;
import single.cjj.botp.adapter.BotpDocumentAdapter;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.RelationStatus;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.TaskStatus;
import single.cjj.botp.domain.BotpContracts.WritebackCommand;
import single.cjj.botp.execution.InMemoryBotpExecutionLogRepository;
import single.cjj.botp.relation.InMemoryBotpRelationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotpWritebackServiceTest {

    @Test
    void shouldRetryWithoutCreatingTargetAgain() {
        FlakySourceAdapter adapter = new FlakySourceAdapter();
        InMemoryBotpWritebackTaskRepository taskRepository = new InMemoryBotpWritebackTaskRepository();
        BotpWritebackService service = new BotpWritebackService(
                taskRepository,
                new BotpAdapterRegistry(List.of(adapter)),
                new InMemoryBotpRelationRepository(),
                new InMemoryBotpExecutionLogRepository()
        );
        DocumentRelation relation = new DocumentRelation(
                1L, "default", "EXEC-2", "RULE", 1,
                new DocumentRef("MATRIX", "FI_AP_DOC", "1001", List.of()),
                new TargetResult("MATRIX", "FI_PAYMENT_APPLICATION", "2001", "PAY-2001"),
                new BigDecimal("500"), RelationStatus.ACTIVE, "ACTIVE", null, null,
                LocalDateTime.now(), null, null
        );
        var task = service.enqueueRecompute("default", "EXEC-2", relation, new BigDecimal("500"));

        service.processTask(task.taskId());
        service.retryNow(task.taskId());
        service.retryNow(task.taskId());

        var result = taskRepository.findById(task.taskId()).orElseThrow();
        assertEquals(TaskStatus.SUCCEEDED, result.status());
        assertEquals(2, result.retryCount());
        assertEquals(3, adapter.calls.get());
    }

    private static final class FlakySourceAdapter implements BotpDocumentAdapter {
        private final AtomicInteger calls = new AtomicInteger();

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
            throw new AssertionError("补偿重试不应创建目标单");
        }

        @Override
        public void applyWriteback(WritebackCommand command) {
            if (calls.incrementAndGet() < 3) {
                throw new IllegalStateException("temporary failure");
            }
        }
    }
}
