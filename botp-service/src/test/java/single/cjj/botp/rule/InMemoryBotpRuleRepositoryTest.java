package single.cjj.botp.rule;

import org.junit.jupiter.api.Test;
import single.cjj.botp.domain.BotpContracts.FieldMapping;
import single.cjj.botp.domain.BotpContracts.MappingSourceType;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.RuleStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryBotpRuleRepositoryTest {

    @Test
    void shouldKeepPublishedVersionsImmutable() {
        InMemoryBotpRuleRepository repository = new InMemoryBotpRuleRepository();
        RuleSaveRequest firstDraft = request("CUSTOM_RULE", "版本一");

        RuleDefinition draftV1 = repository.saveDraft(firstDraft);
        RuleDefinition publishedV1 = repository.publish("CUSTOM_RULE");
        RuleDefinition draftV2 = repository.saveDraft(request("CUSTOM_RULE", "版本二"));
        RuleDefinition publishedV2 = repository.publish("CUSTOM_RULE");

        assertEquals(1, draftV1.version());
        assertEquals(RuleStatus.PUBLISHED, publishedV1.status());
        assertEquals(2, draftV2.version());
        assertEquals(2, publishedV2.version());
        assertEquals("版本一", repository.findVersions("CUSTOM_RULE").get(0).ruleName());
        assertEquals("版本二", repository.findVersions("CUSTOM_RULE").get(1).ruleName());
    }

    private RuleSaveRequest request(String ruleCode, String ruleName) {
        return new RuleSaveRequest(
                ruleCode,
                ruleName,
                "DEMO",
                "DEMO_ORDER",
                "DEMO",
                "DEMO_DELIVERY",
                List.of(new FieldMapping(
                        MappingSourceType.SOURCE_FIELD,
                        "orderNo",
                        "sourceOrderNo",
                        null,
                        true
                )),
                List.of(),
                List.of()
        );
    }
}
