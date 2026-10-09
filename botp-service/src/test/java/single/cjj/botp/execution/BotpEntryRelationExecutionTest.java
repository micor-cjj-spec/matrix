package single.cjj.botp.execution;

import org.junit.jupiter.api.Test;
import single.cjj.botp.adapter.BotpAdapterRegistry;
import single.cjj.botp.adapter.BotpDocumentAdapter;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.ExecutionMode;
import single.cjj.botp.domain.BotpContracts.ExecutionRequest;
import single.cjj.botp.domain.BotpContracts.MappingSourceType;
import single.cjj.botp.domain.BotpContracts.FieldMapping;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.RuleStatus;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetEntryResult;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.engine.BotpMappingEngine;
import single.cjj.botp.rule.BotpRuleRepository;
import single.cjj.botp.rule.RuleSaveRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotpEntryRelationExecutionTest {

    @Test
    void shouldApplyPartialQuantityAndReturnStableTargetEntryCorrelation() {
        SourceAdapter source = new SourceAdapter();
        TargetAdapter target = new TargetAdapter();
        DefaultBotpExecutionService service = new DefaultBotpExecutionService(
                new OneRuleRepository(), new BotpAdapterRegistry(List.of(source, target)), new BotpMappingEngine());

        ExecutionRequest request = new ExecutionRequest(
                "REQ-PARTIAL-1", "TEST", "default", "PO_TO_RECEIPT_TEST",
                List.of(new DocumentRef("MATRIX", "ERP_PURCHASE_ORDER", "1", List.of("11"))),
                Map.of("entryQuantities", Map.of("11", new BigDecimal("60"))),
                ExecutionMode.SYNC, null);

        var result = service.execute(request);
        assertEquals("SUCCEEDED", result.status().name());
        assertEquals(new BigDecimal("60"), target.lastDraft.entries().get(0).get("quantity"));
        assertEquals("11", target.lastDraft.entries().get(0).get("_botpCorrelationKey"));
        assertEquals(1, target.createCount.get());
    }

    private static final class SourceAdapter implements BotpDocumentAdapter {
        @Override public boolean supports(String systemCode, String documentType) {
            return "MATRIX".equals(systemCode) && "ERP_PURCHASE_ORDER".equals(documentType);
        }
        @Override public DocumentData load(DocumentRef ref) {
            return new DocumentData(ref, Map.of("tenantId", "default"), List.of(Map.of(
                    "entryId", "11", "purchaseOrderEntryId", 11L,
                    "availableQuantity", new BigDecimal("100"))));
        }
        @Override public TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TargetAdapter implements BotpDocumentAdapter {
        private final AtomicInteger createCount = new AtomicInteger();
        private TargetDraft lastDraft;
        @Override public boolean supports(String systemCode, String documentType) {
            return "MATRIX".equals(systemCode) && "ERP_PURCHASE_RECEIPT".equals(documentType);
        }
        @Override public DocumentData load(DocumentRef ref) { throw new UnsupportedOperationException(); }
        @Override public TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey) {
            createCount.incrementAndGet();
            lastDraft = targetDraft;
            return new TargetResult("MATRIX", "ERP_PURCHASE_RECEIPT", "2", "PRC-2",
                    List.of(new TargetEntryResult("11", "21")));
        }
    }

    private static final class OneRuleRepository implements BotpRuleRepository {
        private final RuleDefinition rule = new RuleDefinition(
                "PO_TO_RECEIPT_TEST", "test", 1, RuleStatus.PUBLISHED,
                "MATRIX", "ERP_PURCHASE_ORDER", "MATRIX", "ERP_PURCHASE_RECEIPT",
                List.of(),
                List.of(new FieldMapping(MappingSourceType.SOURCE_FIELD,
                        "availableQuantity", "quantity", null, true)),
                List.of());
        @Override public List<RuleDefinition> findAll() { return List.of(rule); }
        @Override public Optional<RuleDefinition> findByCode(String ruleCode) { return Optional.of(rule); }
        @Override public Optional<RuleDefinition> findPublishedByCode(String ruleCode) { return Optional.of(rule); }
        @Override public RuleDefinition saveDraft(RuleSaveRequest request) { throw new UnsupportedOperationException(); }
        @Override public RuleDefinition publish(String ruleCode) { throw new UnsupportedOperationException(); }
        @Override public List<RuleDefinition> findVersions(String ruleCode) { return List.of(rule); }
    }
}
