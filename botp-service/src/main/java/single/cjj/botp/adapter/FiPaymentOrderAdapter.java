package single.cjj.botp.adapter;

import org.springframework.stereotype.Component;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.integration.fi.FiPaymentOrderClient;
import single.cjj.botp.integration.fi.FiPaymentOrderClientContracts.BotpCreateRequest;
import single.cjj.botp.integration.fi.FiPaymentOrderClientContracts.BotpDocument;
import single.cjj.botp.integration.fi.FiPaymentOrderClientContracts.PaymentOrderDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class FiPaymentOrderAdapter implements BotpDocumentAdapter {

    private static final String SYSTEM = "MATRIX";
    private static final String DOCUMENT_TYPE = "FI_PAYMENT_ORDER";

    private final FiPaymentOrderClient client;

    public FiPaymentOrderAdapter(FiPaymentOrderClient client) {
        this.client = client;
    }

    @Override
    public boolean supports(String systemCode, String documentType) {
        return SYSTEM.equals(systemCode) && DOCUMENT_TYPE.equals(documentType);
    }

    @Override
    public DocumentData load(DocumentRef documentRef) {
        BotpDocument doc = requireData(
                client.order(parseOrderId(documentRef.documentId())),
                "读取付款单失败");
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("id", doc.fid());
        header.put("number", doc.number());
        header.put("tenantId", doc.tenantId());
        header.put("orgId", doc.orgId());
        header.put("businessPartnerId", doc.businessPartnerId());
        header.put("currencyCode", doc.currencyCode());
        header.put("amount", doc.amount());
        header.put("status", doc.status());
        header.put("approvalStatus", doc.approvalStatus());
        header.put("paymentMethod", doc.paymentMethod());
        header.put("plannedPayDate", doc.plannedPayDate());
        header.put("payerBankAccountId", doc.payerBankAccountId());
        header.put("payeeBankAccountId", doc.payeeBankAccountId());
        header.put("payeeBankAccountNo", doc.payeeBankAccountNo());
        return new DocumentData(documentRef, header, List.of());
    }

    @Override
    public Optional<TargetResult> findByIdempotencyKey(String idempotencyKey) {
        String tenantId = tenantFromBotpKey(idempotencyKey);
        if (tenantId == null) {
            return Optional.empty();
        }
        ApiResponse<PaymentOrderDetail> response =
                client.findByIdempotency(tenantId, idempotencyKey);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            return Optional.empty();
        }
        return Optional.of(toTarget(response.getData()));
    }

    @Override
    public TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey) {
        Map<String, Object> header = targetDraft.header();
        String sourceDocumentType = text(header.get("sourceDocumentType"), "sourceDocumentType");
        if (!"FI_PAYMENT_APPLICATION".equals(sourceDocumentType)) {
            throw new BizException("付款单只允许由规范付款申请下推");
        }
        BotpCreateRequest request = new BotpCreateRequest(
                idempotencyKey,
                text(header.get("tenantId"), "tenantId"),
                longValue(header.get("orgId"), "orgId"),
                parseApplicationId(text(header.get("sourceDocumentId"), "sourceDocumentId")),
                text(header.get("sourceSystem"), "sourceSystem"),
                sourceDocumentType,
                text(header.get("sourceDocumentId"), "sourceDocumentId"),
                text(header.get("sourceExecutionId"), "sourceExecutionId"),
                decimal(header.get("amount"), "amount"),
                optionalText(header.get("paymentMethod")),
                localDate(header.get("plannedPayDate")),
                optionalText(header.get("payerBankAccountId")),
                nullableLong(header.get("operatorId"))
        );
        return toTarget(requireData(client.create(request), "创建付款单失败"));
    }

    private TargetResult toTarget(PaymentOrderDetail detail) {
        return new TargetResult(
                SYSTEM,
                DOCUMENT_TYPE,
                "PAYORD:" + detail.fid(),
                detail.number()
        );
    }

    private Long parseApplicationId(String value) {
        String normalized = value != null && value.startsWith("PA:")
                ? value.substring(3)
                : value;
        try {
            return Long.valueOf(normalized);
        } catch (Exception exception) {
            throw new BizException("付款申请ID格式错误: " + value);
        }
    }

    private Long parseOrderId(String value) {
        String normalized = value != null && value.startsWith("PAYORD:")
                ? value.substring("PAYORD:".length())
                : value;
        try {
            return Long.valueOf(normalized);
        } catch (Exception exception) {
            throw new BizException("付款单ID格式错误: " + value);
        }
    }

    private String tenantFromBotpKey(String key) {
        if (key == null || !key.startsWith("botp:")) {
            return null;
        }
        int start = "botp:".length();
        int end = key.indexOf(':', start);
        return end <= start ? null : key.substring(start, end);
    }

    private String text(Object value, String field) {
        String result = optionalText(value);
        if (result == null || result.isBlank()) {
            throw new BizException(field + " 不能为空");
        }
        return result;
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

    private Long longValue(Object value, String field) {
        Long result = nullableLong(value);
        if (result == null) {
            throw new BizException(field + " 必须是有效整数");
        }
        return result;
    }

    private Long nullableLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
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

    private <T> T requireData(ApiResponse<T> response, String action) {
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            String message = response == null ? null : response.getMessage();
            throw new BizException(action + (message == null || message.isBlank() ? "" : ": " + message));
        }
        return response.getData();
    }
}
