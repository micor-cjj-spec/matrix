package single.cjj.botp.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.integration.erp.ErpProcurementClient;

import java.util.Map;

@Component
public class ErpPurchaseAcceptanceAdapter extends AbstractErpProcurementAdapter {

    public ErpPurchaseAcceptanceAdapter(
            ErpProcurementClient client,
            @Value("${botp.default-tenant:default}") String tenantId
    ) {
        super(client, tenantId, "ERP_PURCHASE_ACCEPTANCE");
    }

    @Override
    public void validateSource(DocumentData sourceDocument, Map<String, Object> context) {
        if (!"CONFIRMED".equals(status(sourceDocument)) || !"AUDITED".equals(approvalStatus(sourceDocument))) {
            throw new BizException("仅已确认采购验收单允许下推采购入库单");
        }
        if ("REJECTED".equals(String.valueOf(sourceDocument.header().get("result")))) {
            throw new BizException("全部不合格采购验收单不能下推采购入库单");
        }
        requireAvailableEntries(sourceDocument, "采购验收下推入库");
    }

    @Override
    protected boolean canCreateTarget() {
        return true;
    }
}
