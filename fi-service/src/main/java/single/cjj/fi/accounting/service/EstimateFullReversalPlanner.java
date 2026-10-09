package single.cjj.fi.accounting.service;

import single.cjj.bizfi.exception.BizException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EstimateFullReversalPlanner {

    public Plan plan(
            List<InvoiceLine> invoiceLines,
            Map<String, List<EstimateCandidate>> candidatesByPurchaseOrderEntry
    ) {
        if (invoiceLines == null || invoiceLines.isEmpty()) {
            throw new BizException("SUPPLIER_INVOICE_ENTRIES_EMPTY");
        }
        Map<Long, Consumption> consumptionByEstimateEntry = new LinkedHashMap<>();
        List<Allocation> allocations = new ArrayList<>();
        Set<Long> affectedPayables = new LinkedHashSet<>();

        for (InvoiceLine invoice : invoiceLines) {
            BigDecimal remaining = positive(invoice.quantity(), "invoice.quantity");
            List<EstimateCandidate> candidates = candidatesByPurchaseOrderEntry
                    .getOrDefault(invoice.purchaseOrderEntryId(), List.of());

            for (EstimateCandidate candidate : candidates) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                Consumption consumed = consumptionByEstimateEntry.getOrDefault(
                        candidate.estimateEntryId(), new Consumption(BigDecimal.ZERO, BigDecimal.ZERO));
                BigDecimal available = candidate.quantity().subtract(consumed.quantity());
                if (available.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal take = remaining.min(available);
                BigDecimal amount;
                if (consumed.quantity().add(take).compareTo(candidate.quantity()) == 0) {
                    amount = candidate.amount().subtract(consumed.amount()).setScale(2, RoundingMode.HALF_UP);
                } else {
                    amount = take.multiply(candidate.unitPrice()).setScale(2, RoundingMode.HALF_UP);
                }
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BizException("ESTIMATE_ALLOCATION_AMOUNT_INVALID: " + candidate.estimateEntryId());
                }

                Consumption after = new Consumption(
                        consumed.quantity().add(take),
                        consumed.amount().add(amount).setScale(2, RoundingMode.HALF_UP));
                consumptionByEstimateEntry.put(candidate.estimateEntryId(), after);
                affectedPayables.add(candidate.estimatePayableId());
                allocations.add(new Allocation(
                        invoice.invoiceEntryId(),
                        candidate.estimatePayableId(),
                        candidate.estimateEntryId(),
                        take,
                        amount));
                remaining = remaining.subtract(take);
            }

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                throw new BizException("ESTIMATE_ALLOCATION_INSUFFICIENT: invoiceEntry="
                        + invoice.invoiceEntryId() + ", missingQty=" + remaining.stripTrailingZeros().toPlainString());
            }
        }

        return new Plan(
                List.copyOf(allocations),
                Map.copyOf(consumptionByEstimateEntry),
                List.copyOf(affectedPayables));
    }

    private BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(field + " must be > 0");
        }
        return value;
    }

    public record InvoiceLine(
            String invoiceEntryId,
            String purchaseOrderEntryId,
            BigDecimal quantity
    ) {
    }

    public record EstimateCandidate(
            Long estimatePayableId,
            Long estimateEntryId,
            String purchaseOrderEntryId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount
    ) {
    }

    public record Consumption(
            BigDecimal quantity,
            BigDecimal amount
    ) {
    }

    public record Allocation(
            String invoiceEntryId,
            Long estimatePayableId,
            Long estimateEntryId,
            BigDecimal quantity,
            BigDecimal amount
    ) {
    }

    public record Plan(
            List<Allocation> allocations,
            Map<Long, Consumption> consumptionByEstimateEntry,
            List<Long> affectedPayableIds
    ) {
        public Consumption consumption(Long estimateEntryId) {
            return consumptionByEstimateEntry.getOrDefault(
                    estimateEntryId, new Consumption(BigDecimal.ZERO, BigDecimal.ZERO));
        }

        public List<Allocation> allocationsForPayable(Long payableId) {
            return allocations.stream().filter(item -> payableId.equals(item.estimatePayableId())).toList();
        }
    }
}
