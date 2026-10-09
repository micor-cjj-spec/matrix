package single.cjj.botp.relation;

import org.junit.jupiter.api.Test;
import single.cjj.botp.domain.BotpContracts.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotpDocumentGraphServiceTest {

    @Test
    void graphShouldTraverseByFullDocumentKey() {
        InMemoryBotpRelationRepository repository =
                new InMemoryBotpRelationRepository();

        RuleDefinition prToRfq = rule(
                "TRACE_PR_RFQ",
                "ERP_PURCHASE_REQUEST",
                "ERP_PROCUREMENT_RFQ");
        DocumentRelation first = repository.saveActive(
                "T1",
                "TRACE-1",
                prToRfq,
                new DocumentRef(
                        "MATRIX",
                        "ERP_PURCHASE_REQUEST",
                        "100",
                        List.of("101")),
                new TargetResult(
                        "MATRIX",
                        "ERP_PROCUREMENT_RFQ",
                        "200",
                        "RFQ-200"),
                BigDecimal.ZERO
        );
        repository.saveEntries(
                "T1",
                first.relationId(),
                List.of(new DocumentRelationEntry(
                        null, "T1", first.relationId(),
                        "101", "201",
                        new BigDecimal("10"),
                        null, null, null,
                        RelationStatus.ACTIVE))
        );

        RuleDefinition rfqToAward = rule(
                "TRACE_RFQ_AWARD",
                "ERP_PROCUREMENT_RFQ",
                "ERP_SOURCING_AWARD");
        DocumentRelation second = repository.saveActive(
                "T1",
                "TRACE-2",
                rfqToAward,
                new DocumentRef(
                        "MATRIX",
                        "ERP_PROCUREMENT_RFQ",
                        "200",
                        List.of("201")),
                new TargetResult(
                        "MATRIX",
                        "ERP_SOURCING_AWARD",
                        "300",
                        "AWD-300"),
                new BigDecimal("1000")
        );
        repository.saveEntries(
                "T1",
                second.relationId(),
                List.of(new DocumentRelationEntry(
                        null, "T1", second.relationId(),
                        "201", "301",
                        new BigDecimal("10"),
                        new BigDecimal("1000"),
                        null, null,
                        RelationStatus.ACTIVE))
        );

        // Same numeric id under another type must not be mixed into the graph.
        repository.saveActive(
                "T1",
                "TRACE-OTHER",
                rule("OTHER", "ERP_PURCHASE_CONTRACT", "ERP_PURCHASE_ORDER"),
                new DocumentRef(
                        "MATRIX",
                        "ERP_PURCHASE_CONTRACT",
                        "100",
                        List.of()),
                new TargetResult(
                        "MATRIX",
                        "ERP_PURCHASE_ORDER",
                        "999",
                        "PO-999"),
                BigDecimal.ZERO
        );

        BotpDocumentGraphService service =
                new BotpDocumentGraphService(repository);
        DocumentGraph graph = service.graph(
                "T1",
                "MATRIX",
                "ERP_PURCHASE_REQUEST",
                "100",
                10
        );

        assertEquals(3, graph.nodes().size());
        assertEquals(2, graph.edges().size());
        assertFalse(graph.truncated());
        assertTrue(graph.nodes().stream().anyMatch(node ->
                "ERP_SOURCING_AWARD".equals(node.key().documentType())
                        && "300".equals(node.key().documentId())));
        assertFalse(graph.nodes().stream().anyMatch(node ->
                "ERP_PURCHASE_ORDER".equals(node.key().documentType())
                        && "999".equals(node.key().documentId())));
    }

    @Test
    void graphShouldRespectDepthBound() {
        InMemoryBotpRelationRepository repository =
                new InMemoryBotpRelationRepository();
        repository.saveActive(
                "T1", "A",
                rule("A", "TYPE_A", "TYPE_B"),
                new DocumentRef("MATRIX", "TYPE_A", "1", List.of()),
                new TargetResult("MATRIX", "TYPE_B", "2", "B2"),
                BigDecimal.ZERO);
        repository.saveActive(
                "T1", "B",
                rule("B", "TYPE_B", "TYPE_C"),
                new DocumentRef("MATRIX", "TYPE_B", "2", List.of()),
                new TargetResult("MATRIX", "TYPE_C", "3", "C3"),
                BigDecimal.ZERO);

        DocumentGraph graph =
                new BotpDocumentGraphService(repository)
                        .graph("T1", "MATRIX", "TYPE_A", "1", 1);

        assertEquals(2, graph.nodes().size());
        assertEquals(1, graph.edges().size());
    }

    private RuleDefinition rule(
            String code,
            String sourceType,
            String targetType
    ) {
        return new RuleDefinition(
                code, code, 1, RuleStatus.PUBLISHED,
                "MATRIX", sourceType,
                "MATRIX", targetType,
                List.of(), List.of(), List.of());
    }
}
