package single.cjj.botp.rule;

import org.springframework.stereotype.Repository;
import single.cjj.botp.domain.BotpContracts.FieldMapping;
import single.cjj.botp.domain.BotpContracts.MappingSourceType;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.RuleStatus;
import single.cjj.botp.domain.BotpContracts.WritebackMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryBotpRuleRepository implements BotpRuleRepository {

    private final Map<String, RuleDefinition> rules = new LinkedHashMap<>();

    public InMemoryBotpRuleRepository() {
        RuleDefinition demoRule = new RuleDefinition(
                "DEMO_ORDER_TO_DELIVERY",
                "演示订单下推发货单",
                1,
                RuleStatus.PUBLISHED,
                "DEMO",
                "DEMO_ORDER",
                "DEMO",
                "DEMO_DELIVERY",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "orderNo", "sourceOrderNo", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "customerId", "customerId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "operatorId", "createdBy", null, false),
                        new FieldMapping(MappingSourceType.CONSTANT, null, "sourceChannel", "BOTP", true)
                ),
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "materialId", "materialId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "quantity", "deliveryQuantity", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "amount", "deliveryAmount", null, false)
                ),
                List.of(
                        new WritebackMapping("documentNo", "lastTargetNo", "OVERWRITE")
                )
        );
        rules.put(demoRule.ruleCode(), demoRule);
    }

    @Override
    public List<RuleDefinition> findAll() {
        return List.copyOf(rules.values());
    }

    @Override
    public Optional<RuleDefinition> findByCode(String ruleCode) {
        return Optional.ofNullable(rules.get(ruleCode));
    }

    @Override
    public Optional<RuleDefinition> findPublishedByCode(String ruleCode) {
        return findByCode(ruleCode)
                .filter(rule -> rule.status() == RuleStatus.PUBLISHED);
    }
}
