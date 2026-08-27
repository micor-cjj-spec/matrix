package single.cjj.botp.relation;

import org.springframework.stereotype.Service;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.adapter.BotpAdapterRegistry;
import single.cjj.botp.adapter.BotpDocumentAdapter;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.DocumentRelationEntry;
import single.cjj.botp.domain.BotpContracts.RelationStatus;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.RuleStatus;
import single.cjj.botp.domain.BotpContracts.TargetResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ProcurementRelationSyncService {

    private static final String SYSTEM = "MATRIX";

    private final BotpAdapterRegistry adapterRegistry;
    private final BotpRelationRepository repository;

    public ProcurementRelationSyncService(
            BotpAdapterRegistry adapterRegistry,
            BotpRelationRepository repository
    ) {
        this.adapterRegistry = adapterRegistry;
        this.repository = repository;
    }

    public List<DocumentRelation> sync(
            String tenantId,
            String documentType,
            String documentId
    ) {
        String tenant = require(tenantId, "tenantId");
        String type = require(documentType, "documentType");
        String id = require(documentId, "documentId");

        DocumentRef targetRef = new DocumentRef(
                SYSTEM, type, id, List.of());
        BotpDocumentAdapter adapter = adapterRegistry.require(SYSTEM, type);
        DocumentData target = adapter.load(targetRef, tenant);

        List<Lineage> lineage = extract(target);
        if (lineage.isEmpty()) {
            return List.of();
        }

        Map<SourceKey, List<Lineage>> grouped = new LinkedHashMap<>();
        for (Lineage item : lineage) {
            grouped.computeIfAbsent(
                    new SourceKey(item.sourceType(), item.sourceDocumentId()),
                    ignored -> new ArrayList<>()
            ).add(item);
        }

        List<DocumentRelation> result = new ArrayList<>();
        for (Map.Entry<SourceKey, List<Lineage>> group : grouped.entrySet()) {
            SourceKey sourceKey = group.getKey();
            List<Lineage> lines = group.getValue();
            DocumentRef source = new DocumentRef(
                    SYSTEM,
                    sourceKey.sourceType(),
                    sourceKey.sourceDocumentId(),
                    lines.stream()
                            .map(Lineage::sourceEntryId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList()
            );

            String targetNo = text(target.header().get("number"));
            TargetResult targetResult = new TargetResult(
                    SYSTEM, type, id, targetNo);

            RuleDefinition rule = traceRule(
                    sourceKey.sourceType(), type);
            String executionId = stableExecutionId(
                    tenant, sourceKey.sourceType(),
                    sourceKey.sourceDocumentId(), type, id);

            BigDecimal amount = lines.stream()
                    .map(Lineage::amount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            DocumentRelation relation = repository.saveActive(
                    tenant,
                    executionId,
                    rule,
                    source,
                    targetResult,
                    amount
            );

            List<DocumentRelationEntry> entries = new ArrayList<>();
            for (Lineage line : lines) {
                if (line.sourceEntryId() == null
                        || line.targetEntryId() == null) {
                    continue;
                }
                entries.add(new DocumentRelationEntry(
                        null,
                        tenant,
                        relation.relationId(),
                        line.sourceEntryId(),
                        line.targetEntryId(),
                        line.quantity(),
                        line.amount(),
                        null,
                        null,
                        RelationStatus.ACTIVE
                ));
            }
            repository.saveEntries(
                    tenant, relation.relationId(), entries);
            result.add(relation);
        }
        return List.copyOf(result);
    }

    private List<Lineage> extract(DocumentData target) {
        String type = target.reference().documentType();
        return switch (type) {
            case "ERP_PROCUREMENT_RFQ" ->
                    byEntrySource(target,
                            "ERP_PURCHASE_REQUEST",
                            "purchaseRequestId",
                            "purchaseRequestEntryId",
                            "quantity",
                            null);
            case "ERP_SOURCING_AWARD" ->
                    bySingleHeaderSource(target,
                            "ERP_PROCUREMENT_RFQ",
                            "rfqId",
                            "rfqEntryId",
                            "quantity",
                            "amount");
            case "ERP_PURCHASE_CONTRACT" ->
                    bySingleHeaderSource(target,
                            "ERP_SOURCING_AWARD",
                            "sourcingAwardId",
                            "sourcingAwardEntryId",
                            "quantity",
                            "amount");
            case "ERP_PURCHASE_ORDER" ->
                    bySingleHeaderSource(target,
                            "ERP_PURCHASE_CONTRACT",
                            "contractId",
                            "contractEntryId",
                            "quantity",
                            null);
            case "ERP_PURCHASE_DELIVERY_PLAN" ->
                    bySingleHeaderSource(target,
                            "ERP_PURCHASE_ORDER",
                            "purchaseOrderId",
                            "purchaseOrderEntryId",
                            "quantity",
                            null);
            case "ERP_PURCHASE_RECEIPT" ->
                    byEntrySource(target,
                            "ERP_PURCHASE_ORDER",
                            "purchaseOrderId",
                            "purchaseOrderEntryId",
                            "quantity",
                            null);
            case "ERP_PURCHASE_ACCEPTANCE" ->
                    bySingleHeaderSource(target,
                            "ERP_PURCHASE_RECEIPT",
                            "purchaseReceiptId",
                            "purchaseReceiptEntryId",
                            "inspectionQuantity",
                            null);
            case "ERP_PURCHASE_INBOUND" ->
                    bySingleHeaderSource(target,
                            "ERP_PURCHASE_ACCEPTANCE",
                            "purchaseAcceptanceId",
                            "purchaseAcceptanceEntryId",
                            "quantity",
                            "amount");
            case "ERP_PURCHASE_RETURN" ->
                    bySingleHeaderSource(target,
                            "ERP_PURCHASE_INBOUND",
                            "purchaseInboundId",
                            "purchaseInboundEntryId",
                            "quantity",
                            "amount");
            case "ERP_SUPPLIER_CLAIM" ->
                    claimLineage(target);
            case "ERP_PURCHASE_DEDUCTION" ->
                    bySingleHeaderSource(target,
                            "ERP_SUPPLIER_CLAIM",
                            "supplierClaimId",
                            "supplierClaimEntryId",
                            null,
                            "amount");
            default -> List.of();
        };
    }

    private List<Lineage> claimLineage(DocumentData target) {
        String returnId = text(target.header().get("purchaseReturnId"));
        if (returnId != null) {
            List<Lineage> result = new ArrayList<>();
            for (Map<String, Object> entry : target.entries()) {
                String returnEntryId = text(
                        entry.get("purchaseReturnEntryId"));
                if (returnEntryId == null) {
                    continue;
                }
                result.add(new Lineage(
                        "ERP_PURCHASE_RETURN",
                        returnId,
                        returnEntryId,
                        text(entry.get("entryId")),
                        null,
                        decimal(entry.get("amount"))
                ));
            }
            if (!result.isEmpty()) {
                return List.copyOf(result);
            }
        }

        return bySingleHeaderSource(
                target,
                "ERP_PURCHASE_ORDER",
                "purchaseOrderId",
                "purchaseOrderEntryId",
                null,
                "amount"
        );
    }

    private List<Lineage> bySingleHeaderSource(
            DocumentData target,
            String sourceType,
            String sourceHeaderField,
            String sourceEntryField,
            String quantityField,
            String amountField
    ) {
        String sourceDocumentId =
                text(target.header().get(sourceHeaderField));
        if (sourceDocumentId == null) {
            return List.of();
        }
        List<Lineage> result = new ArrayList<>();
        for (Map<String, Object> entry : target.entries()) {
            String sourceEntryId = text(entry.get(sourceEntryField));
            if (sourceEntryId == null) {
                continue;
            }
            result.add(new Lineage(
                    sourceType,
                    sourceDocumentId,
                    sourceEntryId,
                    text(entry.get("entryId")),
                    quantityField == null
                            ? null : decimal(entry.get(quantityField)),
                    amountField == null
                            ? null : decimal(entry.get(amountField))
            ));
        }
        return List.copyOf(result);
    }

    private List<Lineage> byEntrySource(
            DocumentData target,
            String sourceType,
            String sourceDocumentField,
            String sourceEntryField,
            String quantityField,
            String amountField
    ) {
        List<Lineage> result = new ArrayList<>();
        for (Map<String, Object> entry : target.entries()) {
            String sourceDocumentId =
                    text(entry.get(sourceDocumentField));
            String sourceEntryId =
                    text(entry.get(sourceEntryField));
            if (sourceDocumentId == null || sourceEntryId == null) {
                continue;
            }
            result.add(new Lineage(
                    sourceType,
                    sourceDocumentId,
                    sourceEntryId,
                    text(entry.get("entryId")),
                    quantityField == null
                            ? null : decimal(entry.get(quantityField)),
                    amountField == null
                            ? null : decimal(entry.get(amountField))
            ));
        }
        return List.copyOf(result);
    }

    private RuleDefinition traceRule(
            String sourceType,
            String targetType
    ) {
        String code = "TRACE_"
                + shortType(sourceType)
                + "_TO_"
                + shortType(targetType);
        return new RuleDefinition(
                code,
                "Procurement lineage trace",
                1,
                RuleStatus.PUBLISHED,
                SYSTEM,
                sourceType,
                SYSTEM,
                targetType,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private String shortType(String value) {
        String result = value
                .replace("ERP_", "")
                .replace("PROCUREMENT_", "")
                .replace("PURCHASE_", "")
                .replace("SUPPLIER_", "");
        return result.length() <= 20
                ? result
                : result.substring(0, 20);
    }

    private String stableExecutionId(
            String tenantId,
            String sourceType,
            String sourceId,
            String targetType,
            String targetId
    ) {
        String raw = tenantId + "|" + sourceType + "|" + sourceId
                + "|" + targetType + "|" + targetId;
        UUID uuid = UUID.nameUUIDFromBytes(
                raw.getBytes(StandardCharsets.UTF_8));
        return "TRACE-" + uuid.toString().replace("-", "").toUpperCase();
    }

    private BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new BizException(
                    "采购 BOTP 来源数量/金额格式错误: " + value);
        }
    }

    private String text(Object value) {
        if (value == null || String.valueOf(value).isBlank()
                || "null".equalsIgnoreCase(String.valueOf(value))) {
            return null;
        }
        return String.valueOf(value);
    }

    private String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException(field + " 不能为空");
        }
        return value.trim();
    }

    private record SourceKey(
            String sourceType,
            String sourceDocumentId
    ) {
    }

    private record Lineage(
            String sourceType,
            String sourceDocumentId,
            String sourceEntryId,
            String targetEntryId,
            BigDecimal quantity,
            BigDecimal amount
    ) {
    }
}
