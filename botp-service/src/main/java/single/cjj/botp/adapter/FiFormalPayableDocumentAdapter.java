package single.cjj.botp.adapter;

import org.springframework.stereotype.Component;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.WritebackCommand;
import single.cjj.botp.integration.fi.FiPaymentApplicationClient;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.BotpDocument;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FiFormalPayableDocumentAdapter implements BotpDocumentAdapter {

    private static final String SYSTEM = "MATRIX";
    private static final String DOCUMENT_TYPE = "FI_AP_PAYABLE";

    private final FiPaymentApplicationClient client;

    public FiFormalPayableDocumentAdapter(FiPaymentApplicationClient client) {
        this.client = client;
    }

    @Override
    public boolean supports(String systemCode, String documentType) {
        return SYSTEM.equals(systemCode) && DOCUMENT_TYPE.equals(documentType);
    }

    @Override
    public DocumentData load(DocumentRef documentRef) {
        BotpDocument doc = requireData(client.payable(parseId(documentRef.documentId())), "读取正式应付失败");
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("id", doc.fid());
        header.put("number", doc.number());
        header.put("date", doc.date());
        header.put("tenantId", doc.tenantId());
        header.put("orgId", doc.orgId());
        header.put("businessPartnerId", doc.businessPartnerId());
        header.put("businessPartnerCode", doc.businessPartnerCode());
        header.put("businessPartnerName", doc.businessPartnerName());
        header.put("counterparty", doc.businessPartnerId());
        header.put("currencyCode", doc.currencyCode());
        header.put("amount", nz(doc.amount()));
        header.put("openAmount", nz(doc.openAmount()));
        header.put("reservedAmount", nz(doc.reservedAmount()));
        header.put("remainingAmount", nz(doc.availableAmount()));
        header.put("status", doc.status());
        header.put("approvalStatus", doc.approvalStatus());
        header.put("accountingStatus", doc.accountingStatus());
        return new DocumentData(documentRef, header, List.of());
    }

    @Override
    public void validateSource(DocumentData sourceDocument, Map<String, Object> context) {
        String tenant = text(sourceDocument.header().get("tenantId"));
        String requestTenant = text(context.get("tenantId"));
        if (!tenant.equals(requestTenant)) {
            throw new BizException("BOTP租户与正式应付租户不一致");
        }
        String status = text(sourceDocument.header().get("status"));
        if (!"OPEN".equals(status) && !"PARTIAL_SETTLED".equals(status)) {
            throw new BizException("仅未结清正式应付允许下推付款申请");
        }
        if (!"AUDITED".equals(text(sourceDocument.header().get("approvalStatus")))) {
            throw new BizException("仅已审核正式应付允许下推付款申请");
        }
        String accountingStatus = text(sourceDocument.header().get("accountingStatus"));
        if (!"VOUCHER_GENERATED".equals(accountingStatus) && !"POSTED".equals(accountingStatus)) {
            throw new BizException("正式应付核算尚未完成");
        }
        BigDecimal pushAmount = decimal(context.get("pushAmount"), "pushAmount");
        BigDecimal available = decimal(sourceDocument.header().get("remainingAmount"), "remainingAmount");
        if (pushAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("下推金额必须大于0");
        }
        if (pushAmount.compareTo(available) > 0) {
            throw new BizException("下推金额超过应付可申请余额: "
                    + available.stripTrailingZeros().toPlainString());
        }
    }

    @Override
    public TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey) {
        throw new BizException("正式应付适配器不支持创建目标单");
    }

    @Override
    public void applyWriteback(WritebackCommand command) {
        String tenantId = text(command.context().get("tenantId"));
        Long operatorId = nullableLong(command.context().get("operatorId"));
        requireData(client.recomputeReservation(
                parseId(command.sourceDocument().documentId()), tenantId, operatorId),
                "重算正式应付付款占用失败");
    }

    private Long parseId(String value) {
        String normalized = value != null && value.startsWith("AP:") ? value.substring(3) : value;
        try {
            return Long.valueOf(normalized);
        } catch (Exception exception) {
            throw new BizException("正式应付ID格式错误: " + value);
        }
    }

    private BigDecimal decimal(Object value, String field) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception exception) {
            throw new BizException(field + " 必须是有效金额");
        }
    }

    private String text(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BizException("BOTP上下文字段缺失");
        }
        return String.valueOf(value);
    }

    private Long nullableLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> T requireData(ApiResponse<T> response, String action) {
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            String message = response == null ? null : response.getMessage();
            throw new BizException(action + (message == null || message.isBlank() ? "" : ": " + message));
        }
        return response.getData();
    }
}
