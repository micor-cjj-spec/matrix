package single.cjj.botp.adapter;

import org.springframework.stereotype.Component;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.WritebackCommand;
import single.cjj.botp.integration.fi.FiArapClient;
import single.cjj.botp.integration.fi.FiArapClientContracts.ArapWritebackRequest;
import single.cjj.botp.integration.fi.FiArapClientContracts.FiArapDocument;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FiPayableDocumentAdapter implements BotpDocumentAdapter {

    private static final String SYSTEM = "MATRIX";
    private static final String DOCUMENT_TYPE = "FI_AP_DOC";

    private final FiArapClient client;

    public FiPayableDocumentAdapter(FiArapClient client) {
        this.client = client;
    }

    @Override
    public boolean supports(String systemCode, String documentType) {
        return SYSTEM.equals(systemCode) && DOCUMENT_TYPE.equals(documentType);
    }

    @Override
    public DocumentData load(DocumentRef documentRef) {
        FiArapDocument doc = requireData(client.detail(parseId(documentRef.documentId())), "读取应付单失败");
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("id", doc.fid());
        header.put("number", doc.fnumber());
        header.put("date", doc.fdate());
        header.put("counterparty", doc.fcounterparty());
        header.put("amount", nz(doc.famount()));
        header.put("status", doc.fstatus());
        header.put("appliedAmount", nz(doc.fappliedAmount()));
        header.put("reservedAmount", nz(doc.freservedAmount()));
        header.put("remainingAmount", remaining(doc));
        header.put("pushStatus", doc.fpushStatus());
        return new DocumentData(documentRef, header, List.of());
    }

    @Override
    public void validateSource(DocumentData sourceDocument, Map<String, Object> context) {
        if (!"AUDITED".equals(sourceDocument.header().get("status"))) {
            throw new BizException("仅已审核应付单允许下推付款申请");
        }
        BigDecimal pushAmount = decimal(context.get("pushAmount"), "pushAmount");
        BigDecimal remaining = decimal(sourceDocument.header().get("remainingAmount"), "remainingAmount");
        if (pushAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("下推金额必须大于0");
        }
        if (pushAmount.compareTo(remaining) > 0) {
            throw new BizException("下推金额超过应付单剩余金额: " + remaining.stripTrailingZeros().toPlainString());
        }
    }

    @Override
    public TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey) {
        throw new BizException("应付单适配器不支持创建目标单");
    }

    @Override
    public void applyWriteback(WritebackCommand command) {
        BigDecimal activeAmount = decimal(command.context().get("activeAllocatedAmount"), "activeAllocatedAmount");
        BigDecimal releaseAmount = decimal(command.context().get("releaseReservedAmount"), "releaseReservedAmount");
        requireData(client.recomputeWriteback(
                parseId(command.sourceDocument().documentId()),
                new ArapWritebackRequest(activeAmount, releaseAmount, command.executionId())
        ), "应付单反写失败");
    }

    private BigDecimal remaining(FiArapDocument doc) {
        if (doc.fremainingAmount() != null) {
            return doc.fremainingAmount();
        }
        return nz(doc.famount()).subtract(nz(doc.fappliedAmount())).subtract(nz(doc.freservedAmount()));
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BizException("应付单ID格式错误: " + value);
        }
    }

    private BigDecimal decimal(Object value, String field) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new BizException(field + " 必须是有效金额");
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private FiArapDocument requireData(ApiResponse<FiArapDocument> response, String action) {
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            String message = response == null ? null : response.getMessage();
            throw new BizException(action + (message == null || message.isBlank() ? "" : ": " + message));
        }
        return response.getData();
    }
}
