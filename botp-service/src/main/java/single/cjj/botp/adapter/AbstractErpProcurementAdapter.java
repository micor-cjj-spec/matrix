package single.cjj.botp.adapter;

import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetEntryResult;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.integration.erp.ErpProcurementClient;
import single.cjj.botp.integration.erp.ErpProcurementClientContracts.BotpDocumentResponse;
import single.cjj.botp.integration.erp.ErpProcurementClientContracts.BotpTargetCreateRequest;
import single.cjj.botp.integration.erp.ErpProcurementClientContracts.BotpTargetResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

abstract class AbstractErpProcurementAdapter implements BotpDocumentAdapter {

    protected static final String SYSTEM = "MATRIX";

    private final ErpProcurementClient client;
    private final String tenantId;
    private final String documentType;

    protected AbstractErpProcurementAdapter(
            ErpProcurementClient client,
            String tenantId,
            String documentType
    ) {
        this.client = client;
        this.tenantId = tenantId;
        this.documentType = documentType;
    }

    @Override
    public boolean supports(String systemCode, String requestedDocumentType) {
        return SYSTEM.equals(systemCode) && documentType.equals(requestedDocumentType);
    }

    @Override
    public DocumentData load(DocumentRef documentRef) {
        return load(documentRef, tenantId);
    }

    @Override
    public DocumentData load(DocumentRef documentRef, String requestedTenantId) {
        String effectiveTenant = requestedTenantId == null || requestedTenantId.isBlank()
                ? tenantId : requestedTenantId;
        BotpDocumentResponse response = requireData(
                client.document(documentType, parseId(documentRef.documentId()), effectiveTenant),
                "读取采购单据失败"
        );
        List<Map<String, Object>> entries = response.entries() == null ? List.of() : response.entries();
        if (documentRef.entryIds() != null && !documentRef.entryIds().isEmpty()) {
            Set<String> selected = Set.copyOf(documentRef.entryIds());
            entries = entries.stream()
                    .filter(item -> selected.contains(String.valueOf(item.get("entryId"))))
                    .toList();
        }
        return new DocumentData(documentRef, response.header(), entries);
    }

    @Override
    public Optional<TargetResult> findByIdempotencyKey(String idempotencyKey) {
        if (!canCreateTarget()) {
            return Optional.empty();
        }
        ApiResponse<BotpTargetResponse> response = client.findByIdempotency(documentType, idempotencyKey);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            return Optional.empty();
        }
        return Optional.of(toTarget(response.getData()));
    }

    @Override
    public TargetResult createTarget(TargetDraft targetDraft, String idempotencyKey) {
        if (!canCreateTarget()) {
            throw new BizException(documentType + " 不支持作为 BOTP 目标单");
        }
        BotpTargetResponse result = requireData(
                client.createTarget(
                        documentType,
                        new BotpTargetCreateRequest(idempotencyKey, targetDraft.header(), targetDraft.entries())
                ),
                "创建采购目标单失败"
        );
        return toTarget(result);
    }

    protected boolean canCreateTarget() {
        return false;
    }

    protected String status(DocumentData sourceDocument) {
        Object value = sourceDocument.header().get("status");
        return value == null ? null : String.valueOf(value);
    }

    protected String approvalStatus(DocumentData sourceDocument) {
        Object value = sourceDocument.header().get("approvalStatus");
        return value == null ? null : String.valueOf(value);
    }

    protected void requireAvailableEntries(DocumentData sourceDocument, String action) {
        if (sourceDocument.entries().isEmpty()) {
            throw new BizException(action + "至少需要一条可用源分录");
        }
        boolean anyAvailable = sourceDocument.entries().stream()
                .map(item -> item.get("availableQuantity"))
                .anyMatch(this::positiveNumber);
        if (!anyAvailable) {
            throw new BizException(action + "没有可用数量");
        }
    }

    private boolean positiveNumber(Object value) {
        if (value == null) {
            return false;
        }
        try {
            return new java.math.BigDecimal(String.valueOf(value)).compareTo(java.math.BigDecimal.ZERO) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private TargetResult toTarget(BotpTargetResponse target) {
        List<TargetEntryResult> entries = target.entries() == null ? List.of() : target.entries().stream()
                .map(item -> new TargetEntryResult(item.correlationKey(), item.targetEntryId()))
                .toList();
        return new TargetResult(
                target.systemCode(), target.documentType(), target.documentId(), target.documentNo(), entries);
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BizException("采购单据ID格式错误: " + value);
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