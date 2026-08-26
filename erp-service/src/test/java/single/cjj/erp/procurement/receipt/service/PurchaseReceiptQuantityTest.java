package single.cjj.erp.procurement.receipt.service;

import org.junit.jupiter.api.Test;
import single.cjj.erp.procurement.receipt.entity.PurchaseReceiptEntryEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurchaseReceiptQuantityTest {

    @Test
    void availableInspectionShouldExcludeInspectedAndReserved() {
        PurchaseReceiptEntryEntity entry = new PurchaseReceiptEntryEntity();
        entry.setFquantity(new BigDecimal("60"));
        entry.setFinspectedQuantity(new BigDecimal("20"));
        entry.setFinspectionReservedQuantity(new BigDecimal("10"));
        BigDecimal available = entry.getFquantity()
                .subtract(entry.getFinspectedQuantity())
                .subtract(entry.getFinspectionReservedQuantity());
        assertEquals(new BigDecimal("30"), available);
    }
}
