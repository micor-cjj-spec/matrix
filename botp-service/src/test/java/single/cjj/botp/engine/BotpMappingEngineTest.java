package single.cjj.botp.engine;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.FieldMapping;
import single.cjj.botp.domain.BotpContracts.MappingSourceType;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.RuleStatus;
import single.cjj.botp.domain.BotpContracts.TargetDraft;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BotpMappingEngineTest {

    private final BotpMappingEngine mappingEngine = new BotpMappingEngine();

    @Test
    void shouldTransformSourceConstantAndContextMappings() {
        RuleDefinition rule = new RuleDefinition(
                "TEST_RULE",
                "测试规则",
                1,
                RuleStatus.PUBLISHED,
                "TEST",
                "SOURCE",
                "TEST",
                "TARGET",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "customer.id", "customerId", null, true),
                        new FieldMapping(MappingSourceType.CONSTANT, null, "channel", "BOTP", true),
                        new FieldMapping(MappingSourceType.CONTEXT, "operatorId", "createdBy", null, true)
                ),
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "materialId", "materialId", null, true)
                ),
                List.of()
        );
        DocumentRef reference = new DocumentRef("TEST", "SOURCE", "SOURCE-1", List.of());
        DocumentData source = new DocumentData(
                reference,
                Map.of("customer", Map.of("id", "CUSTOMER-1")),
                List.of(Map.of("materialId", "MATERIAL-1"))
        );

        TargetDraft result = mappingEngine.transform(rule, source, Map.of("operatorId", "USER-1"));

        assertEquals("CUSTOMER-1", result.header().get("customerId"));
        assertEquals("BOTP", result.header().get("channel"));
        assertEquals("USER-1", result.header().get("createdBy"));
        assertEquals("MATERIAL-1", result.entries().get(0).get("materialId"));
    }

    @Test
    void shouldRejectEmptyRequiredMapping() {
        RuleDefinition rule = new RuleDefinition(
                "TEST_RULE",
                "测试规则",
                1,
                RuleStatus.PUBLISHED,
                "TEST",
                "SOURCE",
                "TEST",
                "TARGET",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "missing", "requiredField", null, true)
                ),
                List.of(),
                List.of()
        );
        DocumentData source = new DocumentData(
                new DocumentRef("TEST", "SOURCE", "SOURCE-1", List.of()),
                Map.of(),
                List.of()
        );

        assertThrows(BizException.class, () -> mappingEngine.transform(rule, source, Map.of()));
    }
}
