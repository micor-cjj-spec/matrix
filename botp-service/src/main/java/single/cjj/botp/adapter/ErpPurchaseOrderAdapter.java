package single.cjj.botp.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.integration.erp.ErpProcurementClient;

import java.util.Map;

@Component
public class ErpPurchaseOrderAdapter extends AbstractErpProcurementAdapter {

    public ErpPurchaseOrderAdapter(
            ErpProcurementClient client,
            @Value("${botp.default-tenant:default}") String tenantId
    ) {
        super(client, tenantId, "ERP_PURCHASE_ORDER");
    }

    @Override
    public void validateSource(DocumentData sourceDocument, Map<String, Object> context) {
        if (!"EFFECTIVE".equals(status(sourceDocument)) || !"AUDITED".equals(approvalStatus(sourceDocument))) {
            throw new BizException("仅已审核且已生效采购订单允许下推收货单");
        }
        requireAvailableEntries(sourceDocument, "采购订单下推收货");
    }
}
