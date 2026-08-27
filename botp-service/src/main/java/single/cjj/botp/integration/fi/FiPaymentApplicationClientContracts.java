package single.cjj.botp.integration.fi;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class FiPaymentApplicationClientContracts {

    private FiPaymentApplicationClientContracts() {
    }

    public record BotpDocument(
            String documentId,
            Long fid,
            String number,
            LocalDate date,
            String tenantId,
            Long orgId,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            BigDecimal openAmount,
            BigDecimal reservedAmount,
            BigDecimal availableAmount,
            String status,
            String approvalStatus,
            String accountingStatus,
            String paymentMethod,
            LocalDate plannedPayDate,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            String botpIdempotencyKey,
            Integer version
    ) {
    }

    public record PaymentApplicationDetail(
            Long fid,
            String tenantId,
            Long orgId,
            String number,
            LocalDate date,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            LocalDate plannedPayDate,
            String paymentMethod,
            String status,
            String approvalStatus,
            String executionStatus,
            String botpIdempotencyKey,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            Integer version
    ) {
    }

    public record BotpCreateRequest(
            String idempotencyKey,
            String tenantId,
            Long orgId,
            Long payableId,
            String sourceSystem,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            BigDecimal amount,
            String payMethod,
            LocalDate plannedPayDate,
            Long operatorId
    ) {
    }

    public record PayableSnapshot(
            Long fid,
            String tenantId,
            Long orgId,
            String number,
            String type,
            LocalDate date,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            BigDecimal openAmount,
            BigDecimal reservedAmount,
            BigDecimal availableAmount,
            String status,
            String approvalStatus,
            String accountingStatus,
            Integer version
    ) {
    }
}
