package single.cjj.botp.integration.fi;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class FiPaymentOrderClientContracts {

    private FiPaymentOrderClientContracts() {
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

    public record BotpCreateRequest(
            String idempotencyKey,
            String tenantId,
            Long orgId,
            Long paymentApplicationId,
            String sourceSystem,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            BigDecimal amount,
            String paymentMethod,
            LocalDate plannedPayDate,
            String payerBankAccountId,
            Long operatorId
    ) {
    }

    public record PaymentOrderDetail(
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
            String status,
            String approvalStatus,
            String botpIdempotencyKey,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            Integer version
    ) {
    }
}
