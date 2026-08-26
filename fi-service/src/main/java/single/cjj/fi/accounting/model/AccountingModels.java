package single.cjj.fi.accounting.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AccountingModels {

    private AccountingModels() {
    }

    public record RuleHeader(
            Long ruleId,
            Long versionId,
            String ruleCode,
            int versionNo,
            int priority,
            int specificity,
            String source,
            String bookId
    ) {
    }

    public record RuleEntry(
            Long ruleEntryId,
            int lineNo,
            String scope,
            String direction,
            String accountSourceType,
            String accountKey,
            String accountCode,
            String amountExpression,
            String summaryTemplate,
            String currencyExpression
    ) {
    }

    public record RuleDimension(
            Long dimensionId,
            Long ruleEntryId,
            String dimensionCode,
            String sourcePath,
            boolean required
    ) {
    }

    public record AccountMappingCandidate(
            Long mappingId,
            String accountCode,
            int priority,
            int specificity,
            String source
    ) {
    }

    public record DimensionValue(
            String code,
            String valueId,
            String valueCode,
            String valueName
    ) {
    }

    public record AccountingLine(
            int lineNo,
            Long ruleEntryId,
            String sourceEntryId,
            String direction,
            String accountKey,
            String accountCode,
            String summary,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            String currencyCode,
            BigDecimal originalAmount,
            List<DimensionValue> dimensions
    ) {
    }

    public record RuleEvaluation(
            RuleHeader rule,
            List<AccountingLine> lines,
            BigDecimal debitTotal,
            BigDecimal creditTotal
    ) {
    }

    public record ProcessingResult(
            boolean duplicate,
            Long payableId,
            String accountingEventId,
            Long voucherId,
            String voucherNumber
    ) {
    }

    public record EventContext(
            String tenantId,
            Long orgId,
            String bookId,
            LocalDate accountingDate,
            String sourceDocumentNo,
            JsonNode payload
    ) {
    }
}
