package single.cjj.fi.accounting.service;

import org.junit.jupiter.api.Test;
import single.cjj.fi.accounting.persistence.SupplierInvoiceAccountingRepository.OriginalAccountingLine;
import single.cjj.fi.accounting.persistence.SupplierInvoiceAccountingRepository.OriginalDimension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstimateSnapshotAccountingFactoryTest {

    private final EstimateSnapshotAccountingFactory factory = new EstimateSnapshotAccountingFactory();

    @Test
    void fullReversalSwapsDebitAndCreditWithoutChangingAccountsOrDimensions() {
        var lines = original();
        var result = factory.fullReversal(lines, dimensions());

        assertEquals("CREDIT", result.get(0).direction());
        assertEquals("1405", result.get(0).accountCode());
        assertEquals("DEBIT", result.get(1).direction());
        assertEquals("2202", result.get(1).accountCode());
        assertEquals("PROJECT", result.get(0).dimensions().get(0).code());
        assertEquals(0, d("1000").compareTo(result.stream()
                .map(x -> x.debitAmount().add(x.creditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(d("2"))));
    }

    @Test
    void residualRecognitionKeepsOriginalSnapshotAccountsAndUsesResidualAmount() {
        var result = factory.residualRecognition(
                original(),
                dimensions(),
                Map.of("IN-L1", d("400")),
                d("400")
        );

        assertEquals(2, result.size());
        assertEquals("DEBIT", result.get(0).direction());
        assertEquals("1405", result.get(0).accountCode());
        assertEquals(0, d("400").compareTo(result.get(0).debitAmount()));
        assertEquals("CREDIT", result.get(1).direction());
        assertEquals("2202", result.get(1).accountCode());
        assertEquals(0, d("400").compareTo(result.get(1).creditAmount()));
    }

    private List<OriginalAccountingLine> original() {
        return List.of(
                new OriginalAccountingLine(
                        11L, 1, "IN-L1", "DEBIT", "PURCHASE_INBOUND_DEBIT", "1405",
                        "采购入库", d("1000"), d("0"), "CNY", d("1000"), 101L),
                new OriginalAccountingLine(
                        12L, 2, null, "CREDIT", "ESTIMATED_AP", "2202",
                        "暂估应付", d("0"), d("1000"), "CNY", d("1000"), 102L)
        );
    }

    private List<OriginalDimension> dimensions() {
        return List.of(
                new OriginalDimension(11L, "PROJECT", "P1", "P1", "项目1"),
                new OriginalDimension(12L, "BUSINESS_PARTNER", "BP1", "BP1", "供应商1")
        );
    }

    private BigDecimal d(String value) {
        return new BigDecimal(value);
    }
}
