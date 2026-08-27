package single.cjj.fi.ap.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PaymentApplicationContracts {

    private PaymentApplicationContracts() {
    }

    public record AllocationRequest(
            @NotNull Long payableId,
            @NotNull @DecimalMin("0.01") BigDecimal amount
    ) {
    }

    public record EvidenceRequest(
            @NotBlank String evidenceType,
            String sourceSystemCode,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceDocumentNo,
            Boolean required,
            String verificationStatus,
            String remark
    ) {
    }

    public record CreateRequest(
            @NotBlank String tenantId,
            @NotNull Long orgId,
            Long requesterId,
            LocalDate applicationDate,
            LocalDate plannedPayDate,
            String paymentMethod,
            String fundPlanId,
            String payeeBankAccountId,
            String payeeAccountName,
            String payeeBankName,
            String payeeBankAccountNo,
            String remark,
            @NotEmpty List<@Valid AllocationRequest> allocations,
            List<@Valid EvidenceRequest> evidence
    ) {
    }

    public record BudgetCheckRequest(
            @NotBlank String status,
            String checkId,
            String fundPlanId,
            BigDecimal availableAmount,
            String message,
            Object snapshot
    ) {
    }

    public record EvidenceVerifyRequest(
            @NotBlank String verificationStatus,
            String remark
    ) {
    }

    public record ActionRequest(
            Long operatorId,
            String reason
    ) {
    }

    public record BotpCreateRequest(
            @NotBlank String idempotencyKey,
            @NotBlank String tenantId,
            @NotNull Long orgId,
            @NotNull Long payableId,
            @NotBlank String sourceSystem,
            @NotBlank String sourceDocumentType,
            @NotBlank String sourceDocumentId,
            @NotBlank String sourceExecutionId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
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

    public record AllocationView(
            Long fid,
            Long payableId,
            String payableNumber,
            BigDecimal appliedAmount,
            BigDecimal reservedAmount,
            BigDecimal consumedAmount,
            BigDecimal remainingReservedAmount,
            String status
    ) {
    }

    public record EvidenceView(
            Long fid,
            String evidenceType,
            String sourceSystemCode,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceDocumentNo,
            boolean required,
            String verificationStatus,
            String remark
    ) {
    }

    public record Detail(
            Long fid,
            String tenantId,
            Long orgId,
            String number,
            LocalDate date,
            Long requesterId,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            String fundPlanId,
            LocalDate plannedPayDate,
            String paymentMethod,
            String payeeBankAccountId,
            String payeeAccountName,
            String payeeBankName,
            String payeeBankAccountNo,
            String evidenceCheckStatus,
            String budgetCheckStatus,
            String budgetCheckId,
            BigDecimal budgetAvailableAmount,
            String budgetCheckMessage,
            String status,
            String approvalStatus,
            String executionStatus,
            String botpIdempotencyKey,
            String sourceSystemCode,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            String remark,
            String rejectReason,
            Long approvedBy,
            LocalDateTime approvedTime,
            Integer version,
            List<AllocationView> allocations,
            List<EvidenceView> evidence
    ) {
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
}
