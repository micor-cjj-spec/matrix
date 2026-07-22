package single.cjj.botp.adapter;

import org.springframework.stereotype.Component;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.integration.fi.FiArapClient;
import single.cjj.botp.integration.fi.FiArapClientContracts.FiArapDocument;
import single.cjj.botp.integration.fi.FiArapClientContracts.PaymentApplicationCreateRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class FiPaymentApplicationAdapter implements BotpDocumentAdapter {

    private static final String SYSTEM = "MATRIX";
    private static final String DOCUMENT_TYPE = "FI_PAYMENT_APPLICATION";

    private final FiArapClient client;

    public FiPaymentApplicationAdapter(FiArapClient client) {
        this.client = client;
    }

    @Override
    public boolean supports(String systemCode, String documentType) {
        return SYSTEM.equals(systemCode) && DOCUMENT_TYPE.equals(documentType);
    }

    @Override
    public DocumentData load(DocumentRef documentRef) {
        FiArapDocument doc = requireData(client.detail(parseId(documentRef.documentId())), "读取付款申请单失败");
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("id", doc.fid());
        header.put("number", doc.fnumber());
        header.put("counterparty", doc.fcounterparty());
        header.put("amount", doc.famount());
        header.put("status", doc.fstatus());
        header.put("sourceBillNo", doc.fsourceBillNo());
        return new DocumentData(documentRef, header, List.of());
    }

    @Override
    public Optional<TargetResult> findByIdempotencyKey(String idempotencyKey) {
        ApiResponse<FiArapDocument> response = client.findByIdempotency(idempotencyKey);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            return Optional.empty();
        }
        return Optional.of(toTarget(response.getData()));
    }

    @Override
    public TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey) {
        Map<String, Object> header = targetDraft.header();
        PaymentApplicationCreateRequest request = new PaymentApplicationCreateRequest(
                idempotencyKey,
                text(header.get("sourceSystem"), "sourceSystem"),
                text(header.get("sourceDocumentType"), "sourceDocumentType"),
                text(header.get("sourceDocumentId"), "sourceDocumentId"),
                text(header.get("sourceExecutionId"), "sourceExecutionId"),
                text(header.get("sourceBillNo"), "sourceBillNo"),
                text(header.get("counterparty"), "counterparty"),
                decimal(header.get("amount"), "amount"),
                optionalText(header.get("payMethod")),
                localDate(header.get("plannedPayDate")),
                optionalText(header.get("operator"))
        );
        return toTarget(requireData(client.createPaymentApplication(request), "创建付款申请单失败"));
    }

    private TargetResult toTarget(FiArapDocument document) {
        return new TargetResult(
                SYSTEM,
                DOCUMENT_TYPE,
                String.valueOf(document.fid()),
                document.fnumber()
        );
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BizException("付款申请单ID格式错误: " + value);
        }
    }

    private String text(Object value, String field) {
        String text = optionalText(value);
        if (text == null || text.isBlank()) {
            throw new BizException(field + " 不能为空");
        }
        return text;
    }

    private String optionalText(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private LocalDate localDate(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (RuntimeException exception) {
            throw new BizException("plannedPayDate 必须是 yyyy-MM-dd");
        }
    }

    private FiArapDocument requireData(ApiResponse<FiArapDocument> response, String action) {
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            String message = response == null ? null : response.getMessage();
            throw new BizException(action + (message == null || message.isBlank() ? "" : ": " + message));
        }
        return response.getData();
    }
}
