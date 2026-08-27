package single.cjj.erp.integration.fi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class P2pReconciliationContracts {

    private P2pReconciliationContracts() {
    }

    public record ThreeWayMatchRequest(
            String requestId,
            String tenantId,
            Long orgId,
            Long invoiceId,
            String invoiceNo,
            LocalDate invoiceDate,
            Long businessPartnerId,
            String currencyCode,
            List<ThreeWayMatchLine> lines
    ) {
    }

    public record ThreeWayMatchLine(
            Long invoiceEntryId,
            Integer lineNo,
            InvoiceSnapshot invoice,
            PurchaseOrderSnapshot purchaseOrder,
            List<InboundSnapshot> inbounds
    ) {
    }

    public record InvoiceSnapshot(
            Long materialId,
            String materialCode,
            String materialName,
            String specification,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal netAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal grossAmount
    ) {
    }

    public record PurchaseOrderSnapshot(
            Long purchaseOrderId,
            String purchaseOrderNo,
            Long purchaseOrderEntryId,
            Long businessPartnerId,
            String currencyCode,
            Long materialId,
            String materialCode,
            String materialName,
            String specification,
            BigDecimal orderedQuantity,
            BigDecimal inboundQuantity,
            BigDecimal invoicedQuantity,
            BigDecimal unitPrice,
            BigDecimal taxRate
    ) {
    }

    public record InboundSnapshot(
            Long inboundId,
            String inboundNo,
            Long inboundEntryId,
            Long businessPartnerId,
            String currencyCode,
            Long materialId,
            String materialCode,
            String materialName,
            String specification,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            String batchNo
    ) {
    }

    public record ThreeWayMatchResponse(
            Long batchId,
            String batchNo,
            String requestId,
            String result,
            int matchedCount,
            int partialCount,
            int differenceCount,
            int unmatchedCount,
            List<ThreeWayMatchLineResult> lines
    ) {
    }

    public record ThreeWayMatchLineResult(
            Long invoiceEntryId,
            Long caseId,
            String result,
            BigDecimal availableInboundQuantity,
            List<Difference> differences
    ) {
    }

    public record Difference(
            String code,
            String field,
            String expectedValue,
            String actualValue,
            String severity,
            String message
    ) {
    }
}
