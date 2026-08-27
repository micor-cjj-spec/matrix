package single.cjj.fi.accounting.service;

import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.accounting.model.AccountingModels.AccountingLine;
import single.cjj.fi.accounting.model.AccountingModels.DimensionValue;
import single.cjj.fi.accounting.persistence.SupplierInvoiceAccountingRepository.OriginalAccountingLine;
import single.cjj.fi.accounting.persistence.SupplierInvoiceAccountingRepository.OriginalDimension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EstimateSnapshotAccountingFactory {

    public List<AccountingLine> fullReversal(
            List<OriginalAccountingLine> original,
            List<OriginalDimension> dimensions
    ) {
        Map<Long, List<DimensionValue>> dimensionMap = dimensions(dimensions);
        List<AccountingLine> result = new ArrayList<>();
        int lineNo = 1;
        for (OriginalAccountingLine source : original) {
            BigDecimal amount = amount(source);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("ORIGINAL_ESTIMATE_ACCOUNTING_AMOUNT_INVALID: " + source.id());
            }
            String direction = switch (source.direction()) {
                case "DEBIT" -> "CREDIT";
                case "CREDIT" -> "DEBIT";
                default -> throw new BizException("ORIGINAL_ESTIMATE_ACCOUNTING_DIRECTION_INVALID: " + source.direction());
            };
            result.add(line(
                    lineNo++,
                    source,
                    direction,
                    amount,
                    "暂估冲回-" + safe(source.summary()),
                    dimensionMap.getOrDefault(source.id(), List.of())
            ));
        }
        validateBalanced(result, "ESTIMATE_REVERSAL_UNBALANCED");
        return List.copyOf(result);
    }

    public List<AccountingLine> residualRecognition(
            List<OriginalAccountingLine> original,
            List<OriginalDimension> dimensions,
            Map<String, BigDecimal> residualAmountBySourceEntry,
            BigDecimal residualTotal
    ) {
        BigDecimal expectedTotal = nz(residualTotal).setScale(2, RoundingMode.HALF_UP);
        if (expectedTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        Map<Long, List<DimensionValue>> dimensionMap = dimensions(dimensions);
        List<OriginalAccountingLine> sourceLines = original.stream()
                .filter(item -> item.sourceEntryId() != null && !item.sourceEntryId().isBlank())
                .toList();
        List<OriginalAccountingLine> headerLines = original.stream()
                .filter(item -> item.sourceEntryId() == null || item.sourceEntryId().isBlank())
                .toList();
        if (sourceLines.isEmpty() || headerLines.isEmpty()) {
            throw new BizException("ORIGINAL_ESTIMATE_ACCOUNTING_SHAPE_UNSUPPORTED");
        }

        List<AccountingLine> result = new ArrayList<>();
        int lineNo = 1;
        for (OriginalAccountingLine source : sourceLines) {
            BigDecimal amount = nz(residualAmountBySourceEntry.get(source.sourceEntryId()))
                    .setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("RESIDUAL_ESTIMATE_AMOUNT_INVALID: " + source.sourceEntryId());
            }
            result.add(line(
                    lineNo++,
                    source,
                    source.direction(),
                    amount,
                    "残余暂估-" + safe(source.summary()),
                    dimensionMap.getOrDefault(source.id(), List.of())
            ));
        }

        BigDecimal headerOriginalTotal = headerLines.stream()
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (headerOriginalTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("ORIGINAL_ESTIMATE_HEADER_AMOUNT_INVALID");
        }
        BigDecimal assigned = BigDecimal.ZERO;
        for (int i = 0; i < headerLines.size(); i++) {
            OriginalAccountingLine source = headerLines.get(i);
            BigDecimal amount;
            if (i == headerLines.size() - 1) {
                amount = expectedTotal.subtract(assigned);
            } else {
                amount = expectedTotal
                        .multiply(this.amount(source))
                        .divide(headerOriginalTotal, 2, RoundingMode.HALF_UP);
                assigned = assigned.add(amount);
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            result.add(line(
                    lineNo++,
                    source,
                    source.direction(),
                    amount,
                    "残余暂估-" + safe(source.summary()),
                    dimensionMap.getOrDefault(source.id(), List.of())
            ));
        }

        validateBalanced(result, "RESIDUAL_ESTIMATE_ACCOUNTING_UNBALANCED");
        BigDecimal debit = result.stream().map(AccountingLine::debitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debit.setScale(2, RoundingMode.HALF_UP).compareTo(expectedTotal) != 0) {
            throw new BizException("RESIDUAL_ESTIMATE_TOTAL_MISMATCH: expected="
                    + expectedTotal + ", actual=" + debit);
        }
        return List.copyOf(result);
    }

    private AccountingLine line(
            int lineNo,
            OriginalAccountingLine source,
            String direction,
            BigDecimal amount,
            String summary,
            List<DimensionValue> dimensions
    ) {
        boolean debit = "DEBIT".equals(direction);
        boolean credit = "CREDIT".equals(direction);
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        return new AccountingLine(
                lineNo,
                source.ruleEntryId(),
                source.sourceEntryId(),
                direction,
                source.accountKey(),
                source.accountCode(),
                summary,
                debit ? amount : zero,
                credit ? amount : zero,
                source.currencyCode(),
                amount,
                dimensions
        );
    }

    private Map<Long, List<DimensionValue>> dimensions(List<OriginalDimension> dimensions) {
        if (dimensions == null) {
            return Map.of();
        }
        return dimensions.stream().collect(Collectors.groupingBy(
                OriginalDimension::accountingEntryId,
                LinkedHashMap::new,
                Collectors.mapping(item -> new DimensionValue(
                        item.code(), item.valueId(), item.valueCode(), item.valueName()), Collectors.toList())
        ));
    }

    private BigDecimal amount(OriginalAccountingLine line) {
        return nz(line.originalAmount()).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateBalanced(List<AccountingLine> lines, String errorCode) {
        BigDecimal debit = lines.stream().map(AccountingLine::debitAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal credit = lines.stream().map(AccountingLine::creditAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (debit.compareTo(credit) != 0) {
            throw new BizException(errorCode + ": debit=" + debit + ", credit=" + credit);
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
