package single.cjj.botp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.rule.BotpRuleRepository;

import java.util.List;

@RestController
@RequestMapping("/botp/rules")
public class BotpRuleController {

    private final BotpRuleRepository ruleRepository;

    public BotpRuleController(BotpRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @GetMapping
    public ApiResponse<List<RuleDefinition>> listRules() {
        return ApiResponse.success(ruleRepository.findAll());
    }

    @GetMapping("/{ruleCode}")
    public ApiResponse<RuleDefinition> getRule(@PathVariable("ruleCode") String ruleCode) {
        RuleDefinition rule = ruleRepository.findByCode(ruleCode)
                .orElseThrow(() -> new BizException("BOTP 规则不存在: " + ruleCode));
        return ApiResponse.success(rule);
    }
}
