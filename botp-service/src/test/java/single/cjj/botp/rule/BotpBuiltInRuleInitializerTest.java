package single.cjj.botp.rule;

import org.junit.jupiter.api.Test;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;

import static org.junit.jupiter.api.Assertions.*;

class BotpBuiltInRuleInitializerTest {

    @Test
    void shouldPublishCoreProcurementRulesInMemoryMode() throws Exception {
        InMemoryBotpRuleRepository repository =
                new InMemoryBotpRuleRepository();
        BotpBuiltInRuleInitializer initializer =
                new BotpBuiltInRuleInitializer(repository);

        initializer.run(null);

        RuleDefinition contractToOrder =
                repository.findPublishedByCode(
                        "PURCHASE_CONTRACT_TO_ORDER").orElseThrow();
        RuleDefinition orderToReceipt =
                repository.findPublishedByCode(
                        "PURCHASE_ORDER_TO_RECEIPT").orElseThrow();
        RuleDefinition receiptToAcceptance =
                repository.findPublishedByCode(
                        "PURCHASE_RECEIPT_TO_ACCEPTANCE").orElseThrow();
        RuleDefinition acceptanceToInbound =
                repository.findPublishedByCode(
                        "PURCHASE_ACCEPTANCE_TO_INBOUND").orElseThrow();

        assertEquals(
                "ERP_PURCHASE_CONTRACT",
                contractToOrder.sourceDocumentType());
        assertEquals(
                "ERP_PURCHASE_ORDER",
                contractToOrder.targetDocumentType());
        assertEquals(
                "ERP_PURCHASE_RECEIPT",
                orderToReceipt.targetDocumentType());
        assertEquals(
                "ERP_PURCHASE_ACCEPTANCE",
                receiptToAcceptance.targetDocumentType());
        assertEquals(
                "ERP_PURCHASE_INBOUND",
                acceptanceToInbound.targetDocumentType());

        // Initializer is idempotent and should not create a second version.
        initializer.run(null);
        assertEquals(
                1,
                repository.findVersions(
                        "PURCHASE_CONTRACT_TO_ORDER").size());
    }
}
