package single.cjj.botp.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.rule.BotpRuleRepository;
import single.cjj.botp.rule.RuleSaveRequest;

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

    @GetMapping("/{ruleCode}/versions")
    public ApiResponse<List<RuleDefinition>> listVersions(
            @PathVariable("ruleCode") String ruleCode
    ) {
        return ApiResponse.success(ruleRepository.findVersions(ruleCode));
    }

    @PostMapping
    public ApiResponse<RuleDefinition> createDraft(
            @Valid @RequestBody RuleSaveRequest request
    ) {
        return ApiResponse.success("BOTP 规则草稿已保存", ruleRepository.saveDraft(request));
    }

    @PutMapping("/{ruleCode}")
    public ApiResponse<RuleDefinition> updateDraft(
            @PathVariable("ruleCode") String ruleCode,
            @Valid @RequestBody RuleSaveRequest request
    ) {
        if (!ruleCode.equals(request.ruleCode())) {
            throw new BizException("路径规则编码与请求体不一致");
        }
        return ApiResponse.success("BOTP 规则草稿已更新", ruleRepository.saveDraft(request));
    }

    @PostMapping("/{ruleCode}/publish")
    public ApiResponse<RuleDefinition> publish(
            @PathVariable("ruleCode") String ruleCode
    ) {
        return ApiResponse.success("BOTP 规则版本已发布", ruleRepository.publish(ruleCode));
    }
}
