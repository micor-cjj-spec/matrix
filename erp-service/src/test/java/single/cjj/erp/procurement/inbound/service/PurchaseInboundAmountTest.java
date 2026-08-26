package single.cjj.erp.procurement.inbound.service;

import org.junit.jupiter.api.Test;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntryEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** P0-IMP-02 入库金额的最小回归测试。 */
class PurchaseInboundAmountTest {

    @Test
    void shouldUseSourceUnitPriceForInboundAmount() {
        PurchaseInboundEntryEntity entry = new PurchaseInboundEntryEntity();
        entry.setFquantity(new BigDecimal("30"));
        entry.setFunitPrice(new BigDecimal("12.3456"));
        entry.setFamount(entry.getFquantity().multiply(entry.getFunitPrice()).setScale(2, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("370.37"), entry.getFamount());
    }
}
