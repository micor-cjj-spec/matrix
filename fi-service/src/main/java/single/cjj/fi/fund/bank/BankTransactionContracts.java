package single.cjj.fi.fund.bank;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class BankTransactionContracts {

    private BankTransactionContracts() {
    }

    public record CreateRequest(
            @NotBlank String tenantId,
            @NotNull Long orgId,
            @NotBlank String bankAccountId,
            @NotBlank String bankTransactionNo,
            @NotNull LocalDate transactionDate,
            LocalDateTime transactionTime,
            @NotBlank String direction,
            @NotBlank String currencyCode,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String counterpartyName,
            String counterpartyAccount,
            String purpose,
            String summary,
            String bankReceiptNo,
            @NotBlank String sourceChannel,
            String rawPayloadHash,
            Object rawPayload
    ) {
    }

    public record MatchRequest(
            @NotNull Long paymentOrderId,
            Long operatorId
    ) {
    }

    public record Detail(
            Long fid,
            String tenantId,
            Long orgId,
            String bankAccountId,
            String bankTransactionNo,
            LocalDate transactionDate,
            LocalDateTime transactionTime,
            String direction,
            String currencyCode,
            BigDecimal amount,
            String counterpartyName,
            String counterpartyAccount,
            String purpose,
            String summary,
            String bankReceiptNo,
            String sourceChannel,
            String matchStatus,
            String status,
            Long matchedPaymentOrderId,
            Long reconciliationBatchId,
            Long reconciliationCaseId,
            String rawPayloadHash,
            Integer version
    ) {
    }

    public record MatchResult(
            Long bankTransactionId,
            Long paymentOrderId,
            String result,
            Long reconciliationBatchId,
            Long reconciliationCaseId,
            java.util.List<Difference> differences
    ) {
    }

    public record Difference(
            String code,
            String field,
            String expected,
            String actual,
            String message
    ) {
    }
}
