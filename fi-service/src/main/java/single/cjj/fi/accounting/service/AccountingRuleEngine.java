package single.cjj.fi.accounting.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.accounting.model.AccountingModels.AccountMappingCandidate;
import single.cjj.fi.accounting.model.AccountingModels.AccountingLine;
import single.cjj.fi.accounting.model.AccountingModels.DimensionValue;
import single.cjj.fi.accounting.model.AccountingModels.EventContext;
import single.cjj.fi.accounting.model.AccountingModels.RuleDimension;
import single.cjj.fi.accounting.model.AccountingModels.RuleEntry;
import single.cjj.fi.accounting.model.AccountingModels.RuleEvaluation;
import single.cjj.fi.accounting.model.AccountingModels.RuleHeader;
import single.cjj.fi.accounting.persistence.InboundAccountingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountingRuleEngine {

    private final InboundAccountingRepository repository;

    public AccountingRuleEngine(InboundAccountingRepository repository) {
        this.repository = repository;
    }

    public RuleEvaluation evaluate(String accountingEventType, EventContext context) {
        RuleHeader rule = resolveRule(accountingEventType, context);
        List<RuleEntry> definitions = repository.findRuleEntries(rule.versionId());
        if (definitions.isEmpty()) {
            throw new BizException("ACCOUNTING_RULE_EMPTY: " + rule.ruleCode());
        }
        Map<Long, List<RuleDimension>> dimensionsByEntry = repository.findRuleDimensions(rule.versionId())
                .stream().collect(Collectors.groupingBy(RuleDimension::ruleEntryId));

        List<AccountingLine> result = new ArrayList<>();
        int lineNo = 1;
        for (RuleEntry definition : definitions) {
            if ("ENTRY".equals(definition.scope())) {
                JsonNode entries = context.payload().path("entries");
                if (!entries.isArray() || entries.isEmpty()) {
                    throw new BizException("AMOUNT_INVALID: 业务事件缺少 entries");
                }
                for (JsonNode entry : entries) {
                    AccountingLine line = buildLine(lineNo, definition, dimensionsByEntry.getOrDefault(
                            definition.ruleEntryId(), List.of()), context, entry);
                    if (line != null) {
                        result.add(line);
                        lineNo++;
                    }
                }
            } else if ("HEADER".equals(definition.scope())) {
                AccountingLine line = buildLine(lineNo, definition, dimensionsByEntry.getOrDefault(
                        definition.ruleEntryId(), List.of()), context, null);
                if (line != null) {
                    result.add(line);
                    lineNo++;
                }
            } else {
                throw new BizException("ACCOUNTING_RULE_SCOPE_UNSUPPORTED: " + definition.scope());
            }
        }

        BigDecimal debit = result.stream().map(AccountingLine::debitAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal credit = result.stream().map(AccountingLine::creditAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (debit.compareTo(credit) != 0) {
            throw new BizException("ACCOUNTING_UNBALANCED: debit=" + debit + ", credit=" + credit);
        }
        return new RuleEvaluation(rule, List.copyOf(result), debit, credit);
    }

    private RuleHeader resolveRule(String accountingEventType, EventContext context) {
        List<RuleHeader> candidates = repository.findPublishedRules(
                context.tenantId(), context.orgId(), accountingEventType,
                context.bookId(), context.accountingDate());
        if (candidates.isEmpty()) {
            throw new BizException("ACCOUNTING_RULE_NOT_FOUND: " + accountingEventType);
        }
        RuleHeader first = candidates.get(0);
        long sameRank = candidates.stream()
                .filter(item -> item.priority() == first.priority() && item.specificity() == first.specificity())
                .count();
        if (sameRank > 1) {
            throw new BizException("ACCOUNTING_RULE_CONFLICT: " + accountingEventType
                    + ", priority=" + first.priority() + ", specificity=" + first.specificity());
        }
        return first;
    }

    private AccountingLine buildLine(
            int lineNo,
            RuleEntry definition,
            List<RuleDimension> dimensionDefinitions,
            EventContext context,
            JsonNode entry
    ) {
        BigDecimal amount = resolveAmount(definition.amountExpression(), context.payload(), entry)
                .setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) == 0 && definition.skipZeroAmount()) {
            return null;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("AMOUNT_INVALID: accounting line amount must be > 0");
        }
        String accountCode = resolveAccount(definition, context);
        String currency = resolveCurrency(definition.currencyExpression(), context.payload(), entry);
        String summary = renderSummary(definition.summaryTemplate(), context, entry);
        List<DimensionValue> dimensions = dimensionDefinitions.stream()
                .map(item -> resolveDimension(item, context.payload(), entry))
                .filter(item -> item != null)
                .toList();
        boolean debit = "DEBIT".equals(definition.direction());
        boolean credit = "CREDIT".equals(definition.direction());
        if (!debit && !credit) {
            throw new BizException("ACCOUNTING_RULE_DIRECTION_UNSUPPORTED: " + definition.direction());
        }
        return new AccountingLine(
                lineNo,
                definition.ruleEntryId(),
                sourceEntryId(entry),
                definition.direction(),
                definition.accountKey(),
                accountCode,
                summary,
                debit ? amount : BigDecimal.ZERO.setScale(2),
                credit ? amount : BigDecimal.ZERO.setScale(2),
                currency,
                amount,
                dimensions
        );
    }

    private String resolveAccount(RuleEntry definition, EventContext context) {
        if ("FIXED".equals(definition.accountSourceType())) {
            if (definition.accountCode() == null || definition.accountCode().isBlank()) {
                throw new BizException("ACCOUNT_NOT_RESOLVED: fixed account is empty");
            }
            return definition.accountCode();
        }
        if (!"MAPPING".equals(definition.accountSourceType())) {
            throw new BizException("ACCOUNT_SOURCE_TYPE_UNSUPPORTED: " + definition.accountSourceType());
        }
        List<AccountMappingCandidate> candidates = repository.findAccountMappings(
                context.tenantId(), context.orgId(), context.bookId(), definition.accountKey(), context.accountingDate());
        if (candidates.isEmpty()) {
            throw new BizException("ACCOUNT_NOT_RESOLVED: " + definition.accountKey());
        }
        AccountMappingCandidate first = candidates.get(0);
        List<AccountMappingCandidate> sameRank = candidates.stream()
                .filter(item -> item.priority() == first.priority() && item.specificity() == first.specificity())
                .toList();
        if (sameRank.size() > 1) {
            long distinctAccounts = sameRank.stream().map(AccountMappingCandidate::accountCode).distinct().count();
            if (distinctAccounts > 1) {
                throw new BizException("ACCOUNT_MAPPING_CONFLICT: " + definition.accountKey());
            }
        }
        return first.accountCode();
    }

    BigDecimal resolveAmount(String expression, JsonNode payload, JsonNode entry) {
        if (expression == null || expression.isBlank()) {
            throw new BizException("AMOUNT_EXPRESSION_EMPTY");
        }
        if (expression.startsWith("FIELD(") && expression.endsWith(")")) {
            String path = expression.substring(6, expression.length() - 1).trim();
            JsonNode node = readPath(path, payload, entry);
            return decimal(node, expression);
        }
        if (expression.startsWith("SUM(entries.") && expression.endsWith(")")) {
            String field = expression.substring("SUM(entries.".length(), expression.length() - 1).trim();
            JsonNode entries = payload.path("entries");
            if (!entries.isArray()) {
                throw new BizException("AMOUNT_INVALID: entries is not array");
            }
            BigDecimal total = BigDecimal.ZERO;
            for (JsonNode item : entries) {
                total = total.add(decimal(item.get(field), expression));
            }
            return total;
        }
        throw new BizException("AMOUNT_EXPRESSION_UNSUPPORTED: " + expression);
    }

    private String resolveCurrency(String expression, JsonNode payload, JsonNode entry) {
        if (expression == null || expression.isBlank()) {
            return text(payload.get("currencyCode"));
        }
        if (expression.startsWith("FIELD(") && expression.endsWith(")")) {
            return text(readPath(expression.substring(6, expression.length() - 1).trim(), payload, entry));
        }
        if (expression.startsWith("CONST(") && expression.endsWith(")")) {
            return expression.substring(6, expression.length() - 1).trim();
        }
        throw new BizException("CURRENCY_EXPRESSION_UNSUPPORTED: " + expression);
    }

    private DimensionValue resolveDimension(RuleDimension definition, JsonNode payload, JsonNode entry) {
        JsonNode node = readPath(definition.sourcePath(), payload, entry);
        String value = text(node);
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            if (definition.required()) {
                throw new BizException("DIMENSION_MISSING: " + definition.dimensionCode());
            }
            return null;
        }
        String valueCode = null;
        String valueName = null;
        if ("BUSINESS_PARTNER".equals(definition.dimensionCode())) {
            valueCode = text(payload.get("businessPartnerCode"));
            valueName = text(payload.get("businessPartnerName"));
        }
        return new DimensionValue(definition.dimensionCode(), value, valueCode, valueName);
    }

    private String sourceEntryId(JsonNode entry) {
        if (entry == null) {
            return null;
        }
        for (String field : List.of("sourceEntryId", "inboundEntryId", "supplierInvoiceEntryId")) {
            String value = text(entry.get(field));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private JsonNode readPath(String path, JsonNode payload, JsonNode entry) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim();
        JsonNode current;
        if (normalized.startsWith("entry.")) {
            if (entry == null) {
                return null;
            }
            current = entry;
            normalized = normalized.substring("entry.".length());
        } else if (normalized.startsWith("payload.")) {
            current = payload;
            normalized = normalized.substring("payload.".length());
        } else {
            current = payload;
        }
        for (String segment : normalized.split("\\.")) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return null;
            }
            current = current.get(segment);
        }
        return current;
    }

    private String renderSummary(String template, EventContext context, JsonNode entry) {
        String value = template == null || template.isBlank() ? "采购入库暂估" : template;
        value = value.replace("${sourceDocumentNo}", safe(context.sourceDocumentNo()));
        value = value.replace("${businessPartnerName}", safe(text(context.payload().get("businessPartnerName"))));
        value = value.replace("${materialName}", safe(entry == null ? null : text(entry.get("materialName"))));
        return value;
    }

    private BigDecimal decimal(JsonNode node, String expression) {
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new BizException("AMOUNT_INVALID: " + expression);
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException exception) {
            throw new BizException("AMOUNT_INVALID: " + expression + "=" + node.asText());
        }
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
