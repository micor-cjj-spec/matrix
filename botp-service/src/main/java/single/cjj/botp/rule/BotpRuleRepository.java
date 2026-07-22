package single.cjj.botp.rule;

import single.cjj.botp.domain.BotpContracts.RuleDefinition;

import java.util.List;
import java.util.Optional;

public interface BotpRuleRepository {

    List<RuleDefinition> findAll();

    Optional<RuleDefinition> findByCode(String ruleCode);

    Optional<RuleDefinition> findPublishedByCode(String ruleCode);

    RuleDefinition saveDraft(RuleSaveRequest request);

    RuleDefinition publish(String ruleCode);

    List<RuleDefinition> findVersions(String ruleCode);
}
