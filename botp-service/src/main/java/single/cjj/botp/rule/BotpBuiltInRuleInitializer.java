package single.cjj.botp.rule;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import single.cjj.botp.domain.BotpContracts.FieldMapping;
import single.cjj.botp.domain.BotpContracts.MappingSourceType;
import single.cjj.botp.domain.BotpContracts.WritebackMapping;

import java.util.List;

@Component
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class BotpBuiltInRuleInitializer implements ApplicationRunner {

    private final BotpRuleRepository repository;

    public BotpBuiltInRuleInitializer(BotpRuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.findPublishedByCode("AP_TO_PAYMENT_APPLICATION").isPresent()) {
            return;
        }
        repository.saveDraft(new RuleSaveRequest(
                "AP_TO_PAYMENT_APPLICATION",
                "应付单下推付款申请单",
                "MATRIX",
                "FI_AP_DOC",
                "MATRIX",
                "FI_PAYMENT_APPLICATION",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "number", "sourceBillNo", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "counterparty", "counterparty", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceSystemCode", "sourceSystem", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentType", "sourceDocumentType", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentId", "sourceDocumentId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "executionId", "sourceExecutionId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "pushAmount", "amount", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "payMethod", "payMethod", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "plannedPayDate", "plannedPayDate", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "operator", "operator", null, false),
                        new FieldMapping(MappingSourceType.CONSTANT, null, "docType", "AP_PAYMENT_APPLY", true)
                ),
                List.of(),
                List.of(new WritebackMapping("allocatedAmount", "appliedAmount", "RECOMPUTE"))
        ));
        repository.publish("AP_TO_PAYMENT_APPLICATION");
    }
}
