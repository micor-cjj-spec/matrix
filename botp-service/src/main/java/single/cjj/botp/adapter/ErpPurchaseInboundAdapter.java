package single.cjj.botp.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.integration.erp.ErpProcurementClient;

import java.util.Map;

@Component
public class ErpPurchaseInboundAdapter extends AbstractErpProcurementAdapter {

    public ErpPurchaseInboundAdapter(
            ErpProcurementClient client,
            @Value("${botp.default-tenant:default}") String tenantId
    ) {
        super(client, tenantId, "ERP_PURCHASE_INBOUND");
    }

    @Override
    public void validateSource(DocumentData sourceDocument, Map<String, Object> context) {
        // P0-IMP-02 只把采购入库作为 BOTP 可识别目标/源单骨架，后续发票场景再补具体源单校验。
    }

    @Override
    protected boolean canCreateTarget() {
        return true;
    }
}
