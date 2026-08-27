package single.cjj.fi.accounting.service;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.accounting.service.EstimateFullReversalPlanner.EstimateCandidate;
import single.cjj.fi.accounting.service.EstimateFullReversalPlanner.InvoiceLine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstimateFullReversalPlannerTest {

    private final EstimateFullReversalPlanner planner = new EstimateFullReversalPlanner();

    @Test
    void partialInvoiceConsumesPartButAffectsWholeEstimatePayable() {
        var plan = planner.plan(
                List.of(new InvoiceLine("INV-L1", "PO-L1", d("60"))),
                Map.of("PO-L1", List.of(
                        new EstimateCandidate(100L, 101L, "PO-L1", d("100"), d("10"), d("1000"))
                ))
        );

        assertEquals(List.of(100L), plan.affectedPayableIds());
        assertEquals(0, d("60").compareTo(plan.consumption(101L).quantity()));
        assertEquals(0, d("600").compareTo(plan.consumption(101L).amount()));
        assertEquals(0, d("40").compareTo(d("100").subtract(plan.consumption(101L).quantity())));
    }

    @Test
    void invoiceCanConsumeMultipleEstimatePayablesFifo() {
        var plan = planner.plan(
                List.of(new InvoiceLine("INV-L1", "PO-L1", d("70"))),
                Map.of("PO-L1", List.of(
                        new EstimateCandidate(100L, 101L, "PO-L1", d("40"), d("10"), d("400")),
                        new EstimateCandidate(200L, 201L, "PO-L1", d("60"), d("10"), d("600"))
                ))
        );

        assertEquals(List.of(100L, 200L), plan.affectedPayableIds());
        assertEquals(0, d("40").compareTo(plan.consumption(101L).quantity()));
        assertEquals(0, d("30").compareTo(plan.consumption(201L).quantity()));
    }

    @Test
    void finalAllocationUsesRemainingStoredAmountToAvoidRoundingDrift() {
        var plan = planner.plan(
                List.of(
                        new InvoiceLine("INV-L1", "PO-L1", d("1")),
                        new InvoiceLine("INV-L2", "PO-L1", d("2"))
                ),
                Map.of("PO-L1", List.of(
                        new EstimateCandidate(100L, 101L, "PO-L1", d("3"), d("0.333333"), d("1.00"))
                ))
        );

        assertEquals(0, d("1.00").compareTo(plan.consumption(101L).amount()));
    }

    @Test
    void insufficientOpenEstimateQuantityIsRejected() {
        assertThrows(BizException.class, () -> planner.plan(
                List.of(new InvoiceLine("INV-L1", "PO-L1", d("101"))),
                Map.of("PO-L1", List.of(
                        new EstimateCandidate(100L, 101L, "PO-L1", d("100"), d("10"), d("1000"))
                ))
        ));
    }

    private BigDecimal d(String value) {
        return new BigDecimal(value);
    }
}
