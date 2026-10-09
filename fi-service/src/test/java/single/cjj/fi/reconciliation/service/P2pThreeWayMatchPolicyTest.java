package single.cjj.fi.reconciliation.service;

import org.junit.jupiter.api.Test;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.InboundSnapshot;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.InvoiceSnapshot;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.PurchaseOrderSnapshot;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.ThreeWayMatchLine;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.ThreeWayMatchRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P2pThreeWayMatchPolicyTest {

    private final P2pThreeWayMatchPolicy policy = new P2pThreeWayMatchPolicy();

    @Test
    void partialInvoiceWithinConfirmedInboundIsMatched() {
        ThreeWayMatchRequest request = request(
                decimal("60"), decimal("10"), decimal("600"), decimal("0.13"), decimal("78"), decimal("678"),
                decimal("100"), decimal("0")
        );

        P2pThreeWayMatchPolicy.Evaluation result = policy.evaluate(request, request.lines().get(0));

        assertEquals("MATCHED", result.result());
        assertEquals(0, decimal("100").compareTo(result.availableInboundQuantity()));
        assertTrue(result.differences().isEmpty());
    }

    @Test
    void invoiceQuantityCannotExceedConfirmedInboundRemaining() {
        ThreeWayMatchRequest request = request(
                decimal("30"), decimal("10"), decimal("300"), decimal("0.13"), decimal("39"), decimal("339"),
                decimal("100"), decimal("80")
        );

        P2pThreeWayMatchPolicy.Evaluation result = policy.evaluate(request, request.lines().get(0));

        assertEquals("DIFFERENCE", result.result());
        assertEquals(0, decimal("20").compareTo(result.availableInboundQuantity()));
        assertTrue(result.differences().stream().anyMatch(d -> "QUANTITY_DIFFERENCE".equals(d.code())));
    }

    @Test
    void priceDifferenceIsBlocking() {
        ThreeWayMatchRequest request = request(
                decimal("10"), decimal("11"), decimal("110"), decimal("0.13"), decimal("14.30"), decimal("124.30"),
                decimal("100"), decimal("0")
        );

        P2pThreeWayMatchPolicy.Evaluation result = policy.evaluate(request, request.lines().get(0));

        assertEquals("DIFFERENCE", result.result());
        assertTrue(result.differences().stream().anyMatch(d -> "PRICE_DIFFERENCE".equals(d.code())));
        assertTrue(result.differences().stream().allMatch(d -> "BLOCKING".equals(d.severity())));
    }

    @Test
    void missingInboundIsUnmatched() {
        PurchaseOrderSnapshot po = purchaseOrder(decimal("100"), decimal("0"));
        InvoiceSnapshot invoice = invoice(decimal("10"), decimal("10"), decimal("100"), decimal("0.13"), decimal("13"), decimal("113"));
        ThreeWayMatchLine line = new ThreeWayMatchLine(9001L, 1, invoice, po, List.of());
        ThreeWayMatchRequest request = new ThreeWayMatchRequest(
                "REQ-4", "tenant-a", 10L, 8001L, "INV-001", LocalDate.of(2026, 8, 27),
                1001L, "CNY", List.of(line)
        );

        P2pThreeWayMatchPolicy.Evaluation result = policy.evaluate(request, line);

        assertEquals("UNMATCHED", result.result());
        assertTrue(result.differences().stream().anyMatch(d -> "MISSING_DOCUMENT".equals(d.code())));
    }

    private ThreeWayMatchRequest request(
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal netAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal grossAmount,
            BigDecimal inboundQuantity,
            BigDecimal alreadyInvoiced
    ) {
        PurchaseOrderSnapshot po = purchaseOrder(inboundQuantity, alreadyInvoiced);
        InvoiceSnapshot invoice = invoice(quantity, unitPrice, netAmount, taxRate, taxAmount, grossAmount);
        InboundSnapshot inbound = new InboundSnapshot(
                7001L, "PIN-001", 7101L, 1001L, "CNY",
                2001L, "MAT-001", "物料A", "SPEC-A",
                inboundQuantity, decimal("10"), inboundQuantity.multiply(decimal("10")), "BATCH-1"
        );
        ThreeWayMatchLine line = new ThreeWayMatchLine(9001L, 1, invoice, po, List.of(inbound));
        return new ThreeWayMatchRequest(
                "REQ-1", "tenant-a", 10L, 8001L, "INV-001", LocalDate.of(2026, 8, 27),
                1001L, "CNY", List.of(line)
        );
    }

    private PurchaseOrderSnapshot purchaseOrder(BigDecimal inboundQuantity, BigDecimal alreadyInvoiced) {
        return new PurchaseOrderSnapshot(
                5001L, "PO-001", 5101L, 1001L, "CNY",
                2001L, "MAT-001", "物料A", "SPEC-A",
                decimal("100"), inboundQuantity, alreadyInvoiced,
                decimal("10"), decimal("0.13")
        );
    }

    private InvoiceSnapshot invoice(
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal netAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal grossAmount
    ) {
        return new InvoiceSnapshot(
                2001L, "MAT-001", "物料A", "SPEC-A",
                quantity, unitPrice, netAmount, taxRate, taxAmount, grossAmount
        );
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
