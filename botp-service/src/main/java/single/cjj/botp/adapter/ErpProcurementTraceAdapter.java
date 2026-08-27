package single.cjj.botp.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.integration.erp.ErpProcurementClient;
import single.cjj.botp.integration.erp.ErpProcurementClientContracts.BotpDocumentResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ErpProcurementTraceAdapter implements BotpDocumentAdapter {

    private static final String SYSTEM = "MATRIX";
    private static final Set<String> TYPES = Set.of(
            "ERP_PURCHASE_REQUEST",
            "ERP_PROCUREMENT_RFQ",
            "ERP_SOURCING_AWARD",
            "ERP_PURCHASE_CONTRACT",
            "ERP_PURCHASE_DELIVERY_PLAN",
            "ERP_PURCHASE_RETURN",
            "ERP_SUPPLIER_CLAIM",
            "ERP_PURCHASE_DEDUCTION"
    );

    private final ErpProcurementClient client;
    private final String defaultTenant;

    public ErpProcurementTraceAdapter(
            ErpProcurementClient client,
            @Value("${botp.default-tenant:default}") String defaultTenant
    ) {
        this.client = client;
        this.defaultTenant = defaultTenant;
    }

    @Override
    public boolean supports(String systemCode, String documentType) {
        return SYSTEM.equals(systemCode) && TYPES.contains(documentType);
    }

    @Override
    public DocumentData load(DocumentRef documentRef) {
        return load(documentRef, defaultTenant);
    }

    @Override
    public DocumentData load(DocumentRef documentRef, String tenantId) {
        String effectiveTenant = tenantId == null || tenantId.isBlank()
                ? defaultTenant : tenantId;
        ApiResponse<BotpDocumentResponse> api = client.document(
                documentRef.documentType(),
                parseId(documentRef.documentId()),
                effectiveTenant
        );
        if (api == null || api.getCode() != 200 || api.getData() == null) {
            throw new BizException("读取采购 BOTP 追溯单据失败: "
                    + documentRef.documentType() + "/" + documentRef.documentId());
        }
        BotpDocumentResponse response = api.getData();
        List<Map<String, Object>> entries =
                response.entries() == null ? List.of() : response.entries();
        if (documentRef.entryIds() != null && !documentRef.entryIds().isEmpty()) {
            Set<String> selected = Set.copyOf(documentRef.entryIds());
            entries = entries.stream()
                    .filter(item -> selected.contains(
                            String.valueOf(item.get("entryId"))))
                    .toList();
        }
        return new DocumentData(documentRef, response.header(), entries);
    }

    @Override
    public TargetResult createTarget(
            TargetDraft targetDraft,
            String idempotencyKey
    ) {
        throw new BizException(
                targetDraft.documentType()
                        + " 当前作为采购追溯单据注册，不支持通用 BOTP 自动创建");
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BizException("采购单据ID格式错误: " + value);
        }
    }
}
