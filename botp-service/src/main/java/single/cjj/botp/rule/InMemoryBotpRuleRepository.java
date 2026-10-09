package single.cjj.botp.rule;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.FieldMapping;
import single.cjj.botp.domain.BotpContracts.MappingSourceType;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.RuleStatus;
import single.cjj.botp.domain.BotpContracts.WritebackMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "memory")
public class InMemoryBotpRuleRepository implements BotpRuleRepository {

    private final Map<String, RuleDefinition> drafts = new LinkedHashMap<>();
    private final Map<String, List<RuleDefinition>> publishedVersions = new LinkedHashMap<>();

    public InMemoryBotpRuleRepository() {
        registerPublished(demoRule());
        registerPublished(apToPaymentApplicationRule());
        registerPublished(formalApToPaymentApplicationRule());
        registerPublished(paymentApplicationToOrderRule());
    }

    @Override
    public synchronized List<RuleDefinition> findAll() {
        Set<String> ruleCodes = new LinkedHashSet<>();
        ruleCodes.addAll(publishedVersions.keySet());
        ruleCodes.addAll(drafts.keySet());
        return ruleCodes.stream()
                .map(this::findByCode)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public synchronized Optional<RuleDefinition> findByCode(String ruleCode) {
        RuleDefinition draft = drafts.get(ruleCode);
        return draft != null ? Optional.of(draft) : findPublishedByCode(ruleCode);
    }

    @Override
    public synchronized Optional<RuleDefinition> findPublishedByCode(String ruleCode) {
        List<RuleDefinition> versions = publishedVersions.get(ruleCode);
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(versions.get(versions.size() - 1));
    }

    @Override
    public synchronized RuleDefinition saveDraft(RuleSaveRequest request) {
        int version = Optional.ofNullable(drafts.get(request.ruleCode()))
                .map(RuleDefinition::version)
                .orElseGet(() -> findPublishedByCode(request.ruleCode())
                        .map(published -> published.version() + 1)
                        .orElse(1));

        RuleDefinition draft = new RuleDefinition(
                request.ruleCode(),
                request.ruleName(),
                version,
                RuleStatus.DRAFT,
                request.sourceSystemCode(),
                request.sourceDocumentType(),
                request.targetSystemCode(),
                request.targetDocumentType(),
                request.headerMappings(),
                request.entryMappings(),
                request.writebackMappings()
        );
        drafts.put(draft.ruleCode(), draft);
        return draft;
    }

    @Override
    public synchronized RuleDefinition publish(String ruleCode) {
        RuleDefinition draft = drafts.remove(ruleCode);
        if (draft == null) {
            throw new BizException("BOTP 待发布草稿不存在: " + ruleCode);
        }
        RuleDefinition published = new RuleDefinition(
                draft.ruleCode(),
                draft.ruleName(),
                draft.version(),
                RuleStatus.PUBLISHED,
                draft.sourceSystemCode(),
                draft.sourceDocumentType(),
                draft.targetSystemCode(),
                draft.targetDocumentType(),
                draft.headerMappings(),
                draft.entryMappings(),
                draft.writebackMappings()
        );
        registerPublished(published);
        return published;
    }

    @Override
    public synchronized List<RuleDefinition> findVersions(String ruleCode) {
        List<RuleDefinition> versions = publishedVersions.get(ruleCode);
        return versions == null ? List.of() : List.copyOf(versions);
    }

    private void registerPublished(RuleDefinition rule) {
        publishedVersions.computeIfAbsent(rule.ruleCode(), key -> new ArrayList<>()).add(rule);
    }

    private RuleDefinition demoRule() {
        return new RuleDefinition(
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
                List.of(new WritebackMapping("documentNo", "lastTargetNo", "OVERWRITE"))
        );
    }

    private RuleDefinition formalApToPaymentApplicationRule() {
        return new RuleDefinition(
                "FORMAL_AP_TO_PAYMENT_APPLICATION",
                "正式应付下推付款申请",
                1,
                RuleStatus.PUBLISHED,
                "MATRIX",
                "FI_AP_PAYABLE",
                "MATRIX",
                "FI_PAYMENT_APPLICATION",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "number", "sourceBillNo", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerId", "counterparty", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "orgId", "orgId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "tenantId", "tenantId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceSystemCode", "sourceSystem", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentType", "sourceDocumentType", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentId", "sourceDocumentId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "executionId", "sourceExecutionId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "pushAmount", "amount", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "payMethod", "payMethod", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "plannedPayDate", "plannedPayDate", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "operatorId", "operatorId", null, false)
                ),
                List.of(),
                List.of(new WritebackMapping("allocatedAmount", "reservedAmount", "RECOMPUTE"))
        );
    }

    private RuleDefinition paymentApplicationToOrderRule() {
        return new RuleDefinition(
                "PAYMENT_APPLICATION_TO_PAYMENT_ORDER",
                "付款申请下推付款单",
                1,
                RuleStatus.PUBLISHED,
                "MATRIX",
                "FI_PAYMENT_APPLICATION",
                "MATRIX",
                "FI_PAYMENT_ORDER",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "number", "sourceBillNo", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "orgId", "orgId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "paymentMethod", "paymentMethod", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "plannedPayDate", "plannedPayDate", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "tenantId", "tenantId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceSystemCode", "sourceSystem", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentType", "sourceDocumentType", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentId", "sourceDocumentId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "executionId", "sourceExecutionId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "pushAmount", "amount", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "payerBankAccountId", "payerBankAccountId", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "operatorId", "operatorId", null, false)
                ),
                List.of(),
                List.of(new WritebackMapping("allocatedAmount", "orderedAmount", "RECOMPUTE"))
        );
    }

    private RuleDefinition apToPaymentApplicationRule() {
        return new RuleDefinition(
                "AP_TO_PAYMENT_APPLICATION",
                "应付单下推付款申请单",
                1,
                RuleStatus.PUBLISHED,
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
        );
    }
}
