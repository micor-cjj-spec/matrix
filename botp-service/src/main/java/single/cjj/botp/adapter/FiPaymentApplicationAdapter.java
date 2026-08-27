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
import single.cjj.botp.integration.fi.FiPaymentApplicationClient;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.BotpCreateRequest;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.BotpDocument;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.PaymentApplicationDetail;

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
    private static final String CANONICAL_SOURCE_TYPE = "FI_AP_PAYABLE";

    private final FiArapClient legacyClient;
    private final FiPaymentApplicationClient canonicalClient;

    public FiPaymentApplicationAdapter(
            FiArapClient legacyClient,
            FiPaymentApplicationClient canonicalClient
    ) {
        this.legacyClient = legacyClient;
        this.canonicalClient = canonicalClient;
    }

    @Override
    public boolean supports(String systemCode, String documentType) {
        return SYSTEM.equals(systemCode) && DOCUMENT_TYPE.equals(documentType);
    }

    @Override
    public DocumentData load(DocumentRef documentRef) {
        if (isCanonicalTarget(documentRef.documentId())) {
            BotpDocument doc = requireData(
                    canonicalClient.application(parseCanonicalId(documentRef.documentId())),
                    "读取规范付款申请失败");
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("id", doc.fid());
            header.put("number", doc.number());
            header.put("tenantId", doc.tenantId());
            header.put("orgId", doc.orgId());
            header.put("counterparty", doc.businessPartnerId());
            header.put("currencyCode", doc.currencyCode());
            header.put("amount", doc.amount());
            header.put("status", doc.status());
            header.put("approvalStatus", doc.approvalStatus());
            header.put("paymentMethod", doc.paymentMethod());
            header.put("plannedPayDate", doc.plannedPayDate());
            header.put("sourceDocumentType", doc.sourceDocumentType());
            header.put("sourceDocumentId", doc.sourceDocumentId());
            return new DocumentData(documentRef, header, List.of());
        }

        FiArapDocument doc = requireData(
                legacyClient.detail(parseLegacyId(documentRef.documentId())),
                "读取历史付款申请单失败");
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
        String tenantId = tenantFromBotpKey(idempotencyKey);
        if (tenantId != null) {
            ApiResponse<PaymentApplicationDetail> canonical =
                    canonicalClient.findByIdempotency(tenantId, idempotencyKey);
            if (canonical != null && canonical.getCode() == 200 && canonical.getData() != null) {
                return Optional.of(toCanonicalTarget(canonical.getData()));
            }
        }

        ApiResponse<FiArapDocument> legacy = legacyClient.findByIdempotency(idempotencyKey);
        if (legacy == null || legacy.getCode() != 200 || legacy.getData() == null) {
            return Optional.empty();
        }
        return Optional.of(toLegacyTarget(legacy.getData()));
    }

    @Override
    public TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey) {
        Map<String, Object> header = targetDraft.header();
        String sourceDocumentType = text(header.get("sourceDocumentType"), "sourceDocumentType");
        if (CANONICAL_SOURCE_TYPE.equals(sourceDocumentType)) {
            BotpCreateRequest request = new BotpCreateRequest(
                    idempotencyKey,
                    text(header.get("tenantId"), "tenantId"),
                    longValue(header.get("orgId"), "orgId"),
                    parseCanonicalPayableId(text(header.get("sourceDocumentId"), "sourceDocumentId")),
                    text(header.get("sourceSystem"), "sourceSystem"),
                    sourceDocumentType,
                    text(header.get("sourceDocumentId"), "sourceDocumentId"),
                    text(header.get("sourceExecutionId"), "sourceExecutionId"),
                    decimal(header.get("amount"), "amount"),
                    optionalText(header.get("payMethod")),
                    localDate(header.get("plannedPayDate")),
                    nullableLong(header.get("operatorId") != null
                            ? header.get("operatorId")
                            : header.get("operator"))
            );
            return toCanonicalTarget(requireData(
                    canonicalClient.create(request),
                    "创建规范付款申请失败"));
        }

        PaymentApplicationCreateRequest request = new PaymentApplicationCreateRequest(
                idempotencyKey,
                text(header.get("sourceSystem"), "sourceSystem"),
                sourceDocumentType,
                text(header.get("sourceDocumentId"), "sourceDocumentId"),
                text(header.get("sourceExecutionId"), "sourceExecutionId"),
                text(header.get("sourceBillNo"), "sourceBillNo"),
                text(header.get("counterparty"), "counterparty"),
                decimal(header.get("amount"), "amount"),
                optionalText(header.get("payMethod")),
                localDate(header.get("plannedPayDate")),
                optionalText(header.get("operator"))
        );
        return toLegacyTarget(requireData(
                legacyClient.createPaymentApplication(request),
                "创建历史付款申请失败"));
    }

    private TargetResult toCanonicalTarget(PaymentApplicationDetail document) {
        return new TargetResult(
                SYSTEM,
                DOCUMENT_TYPE,
                "PA:" + document.fid(),
                document.number()
        );
    }

    private TargetResult toLegacyTarget(FiArapDocument document) {
        return new TargetResult(
                SYSTEM,
                DOCUMENT_TYPE,
                String.valueOf(document.fid()),
                document.fnumber()
        );
    }

    private boolean isCanonicalTarget(String documentId) {
        return documentId != null && documentId.startsWith("PA:");
    }

    private Long parseCanonicalId(String value) {
        try {
            return Long.valueOf(value.substring(3));
        } catch (Exception exception) {
            throw new BizException("规范付款申请ID格式错误: " + value);
        }
    }

    private Long parseCanonicalPayableId(String value) {
        String normalized = value != null && value.startsWith("AP:") ? value.substring(3) : value;
        try {
            return Long.valueOf(normalized);
        } catch (Exception exception) {
            throw new BizException("正式应付ID格式错误: " + value);
        }
    }

    private Long parseLegacyId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BizException("历史付款申请单ID格式错误: " + value);
        }
    }

    private String tenantFromBotpKey(String key) {
        if (key == null || !key.startsWith("botp:")) {
            return null;
        }
        int start = "botp:".length();
        int end = key.indexOf(':', start);
        if (end <= start) {
            return null;
        }
        return key.substring(start, end);
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
