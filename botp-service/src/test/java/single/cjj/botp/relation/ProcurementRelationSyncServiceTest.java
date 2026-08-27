package single.cjj.botp.relation;

import org.junit.jupiter.api.Test;
import single.cjj.botp.adapter.BotpAdapterRegistry;
import single.cjj.botp.adapter.BotpDocumentAdapter;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcurementRelationSyncServiceTest {

    @Test
    void shouldSyncContractAwardLineageAndRemainIdempotent() {
        BotpAdapterRegistry registry = mock(BotpAdapterRegistry.class);
        BotpDocumentAdapter adapter = mock(BotpDocumentAdapter.class);
        when(registry.require("MATRIX", "ERP_PURCHASE_CONTRACT"))
                .thenReturn(adapter);

        DocumentRef targetRef = new DocumentRef(
                "MATRIX", "ERP_PURCHASE_CONTRACT", "400", List.of());
        DocumentData target = new DocumentData(
                targetRef,
                Map.of(
                        "number", "PC-400",
                        "sourcingAwardId", 300L
                ),
                List.of(
                        Map.of(
                                "entryId", "401",
                                "sourcingAwardEntryId", 301L,
                                "quantity", new BigDecimal("4"),
                                "amount", new BigDecimal("400.00")
                        ),
                        Map.of(
                                "entryId", "402",
                                "sourcingAwardEntryId", 302L,
                                "quantity", new BigDecimal("6"),
                                "amount", new BigDecimal("600.00")
                        )
                )
        );
        when(adapter.load(any(DocumentRef.class), eq("T1")))
                .thenReturn(target);

        InMemoryBotpRelationRepository repository =
                new InMemoryBotpRelationRepository();
        ProcurementRelationSyncService service =
                new ProcurementRelationSyncService(registry, repository);

        List<DocumentRelation> first =
                service.sync("T1", "ERP_PURCHASE_CONTRACT", "400");
        List<DocumentRelation> second =
                service.sync("T1", "ERP_PURCHASE_CONTRACT", "400");

        assertEquals(1, first.size());
        assertEquals(first.get(0).relationId(), second.get(0).relationId());
        assertEquals(
                "ERP_SOURCING_AWARD",
                first.get(0).sourceDocument().documentType());
        assertEquals("300", first.get(0).sourceDocument().documentId());
        assertEquals(new BigDecimal("1000.00"), first.get(0).allocatedAmount());
        assertEquals(
                2,
                repository.findEntries(first.get(0).relationId()).size());
        assertEquals(
                1,
                repository.findByTarget(
                        "T1",
                        "MATRIX",
                        "ERP_PURCHASE_CONTRACT",
                        "400").size());
    }

    @Test
    void claimShouldPreferReturnLineageWhenReturnExists() {
        BotpAdapterRegistry registry = mock(BotpAdapterRegistry.class);
        BotpDocumentAdapter adapter = mock(BotpDocumentAdapter.class);
        when(registry.require("MATRIX", "ERP_SUPPLIER_CLAIM"))
                .thenReturn(adapter);

        when(adapter.load(any(DocumentRef.class), eq("T1")))
                .thenReturn(new DocumentData(
                        new DocumentRef(
                                "MATRIX",
                                "ERP_SUPPLIER_CLAIM",
                                "700",
                                List.of()),
                        Map.of(
                                "number", "CLM-700",
                                "purchaseReturnId", 600L,
                                "purchaseOrderId", 100L
                        ),
                        List.of(Map.of(
                                "entryId", "701",
                                "purchaseReturnEntryId", 601L,
                                "purchaseOrderEntryId", 101L,
                                "amount", new BigDecimal("50.00")
                        ))
                ));

        InMemoryBotpRelationRepository repository =
                new InMemoryBotpRelationRepository();
        List<DocumentRelation> relations =
                new ProcurementRelationSyncService(registry, repository)
                        .sync("T1", "ERP_SUPPLIER_CLAIM", "700");

        assertEquals(1, relations.size());
        assertEquals(
                "ERP_PURCHASE_RETURN",
                relations.get(0).sourceDocument().documentType());
        assertEquals("600", relations.get(0).sourceDocument().documentId());
    }
}
