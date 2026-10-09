package single.cjj.fi.fund.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PaymentOrderContracts {

    private PaymentOrderContracts() {
    }

    public record AllocationRequest(
            @NotNull Long paymentApplicationId,
            @NotNull @DecimalMin("0.01") BigDecimal amount
    ) {
    }

    public record CreateRequest(
            @NotBlank String tenantId,
            @NotNull Long orgId,
            LocalDate orderDate,
            @NotBlank String paymentMethod,
            String payerBankAccountId,
            String payeeBankAccountId,
            String payeeAccountName,
            String payeeBankName,
            String payeeBankAccountNo,
            String fundPlanId,
            LocalDate plannedPayDate,
            String remark,
            @NotEmpty List<@Valid AllocationRequest> allocations
    ) {
    }

    public record LiquidityCheckRequest(
            @NotBlank String status,
            String checkId,
            BigDecimal availableAmount,
            String message,
            Object snapshot
    ) {
    }

    public record ActionRequest(
            Long operatorId,
            String reason
    ) {
    }

    public record SubmitToBankRequest(
            Long operatorId,
            @NotBlank String channelCode,
            String channelRequestId
    ) {
    }

    public record ChannelFailureRequest(
            Long operatorId,
            String errorMessage
    ) {
    }

    public record BotpCreateRequest(
            @NotBlank String idempotencyKey,
            @NotBlank String tenantId,
            @NotNull Long orgId,
            @NotNull Long paymentApplicationId,
            @NotBlank String sourceSystem,
            @NotBlank String sourceDocumentType,
            @NotBlank String sourceDocumentId,
            @NotBlank String sourceExecutionId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String paymentMethod,
            LocalDate plannedPayDate,
            String payerBankAccountId,
            Long operatorId
    ) {
    }

    public record AllocationView(
            Long fid,
            Long paymentApplicationId,
            String paymentApplicationNo,
            BigDecimal amount,
            String status
    ) {
    }

    public record Detail(
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
            String paymentMethod,
            String payerBankAccountId,
            String payeeBankAccountId,
            String payeeAccountName,
            String payeeBankName,
            String payeeBankAccountNo,
            String fundPlanId,
            LocalDate plannedPayDate,
            String liquidityCheckStatus,
            String liquidityCheckId,
            BigDecimal liquidityAvailableAmount,
            String liquidityCheckMessage,
            String status,
            String approvalStatus,
            String channelCode,
            String channelRequestId,
            String channelStatus,
            String channelError,
            LocalDateTime submittedTime,
            Long auditBy,
            LocalDateTime auditTime,
            LocalDateTime payingTime,
            String bankMatchStatus,
            Long reconciliationBatchId,
            Long reconciliationCaseId,
            String botpIdempotencyKey,
            String sourceSystemCode,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            String remark,
            String rejectReason,
            Integer version,
            List<AllocationView> allocations
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
            BigDecimal orderedAmount,
            BigDecimal availableOrderAmount,
            String status,
            String approvalStatus,
            String executionStatus,
            String paymentMethod,
            LocalDate plannedPayDate,
            String payerBankAccountId,
            String payeeBankAccountId,
            String payeeAccountName,
            String payeeBankName,
            String payeeBankAccountNo,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            String botpIdempotencyKey,
            Integer version
    ) {
    }
}
