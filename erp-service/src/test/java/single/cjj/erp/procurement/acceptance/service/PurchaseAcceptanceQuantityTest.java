package single.cjj.erp.procurement.acceptance.service;

import org.junit.jupiter.api.Test;
import single.cjj.erp.procurement.acceptance.entity.PurchaseAcceptanceEntryEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** P0-IMP-02 验收数量模型的最小回归测试。 */
class PurchaseAcceptanceQuantityTest {

    @Test
    void acceptedAndRejectedShouldBalanceInspectionQuantity() {
        PurchaseAcceptanceEntryEntity entry = new PurchaseAcceptanceEntryEntity();
        entry.setFinspectionQuantity(new BigDecimal("60"));
        entry.setFqualifiedQuantity(new BigDecimal("45"));
        entry.setFconcessionQuantity(new BigDecimal("5"));
        entry.setFrejectedQuantity(new BigDecimal("10"));

        BigDecimal result = entry.getFqualifiedQuantity()
                .add(entry.getFconcessionQuantity())
                .add(entry.getFrejectedQuantity());
        assertEquals(entry.getFinspectionQuantity(), result);
        assertEquals(new BigDecimal("50"), entry.getFqualifiedQuantity().add(entry.getFconcessionQuantity()));
    }
}
