package single.cjj.botp.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.integration.erp.ErpProcurementClient;

import java.util.Map;

@Component
public class ErpPurchaseReceiptAdapter extends AbstractErpProcurementAdapter {

    public ErpPurchaseReceiptAdapter(
            ErpProcurementClient client,
            @Value("${botp.default-tenant:default}") String tenantId
    ) {
        super(client, tenantId, "ERP_PURCHASE_RECEIPT");
    }

    @Override
    public void validateSource(DocumentData sourceDocument, Map<String, Object> context) {
        if (!"CONFIRMED".equals(status(sourceDocument)) || !"AUDITED".equals(approvalStatus(sourceDocument))) {
            throw new BizException("仅已确认采购收货单允许下推验收单");
        }
        requireAvailableEntries(sourceDocument, "采购收货下推验收");
    }

    @Override
    protected boolean canCreateTarget() {
        return true;
    }
}
