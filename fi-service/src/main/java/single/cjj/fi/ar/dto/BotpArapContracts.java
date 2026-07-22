package single.cjj.fi.ar.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BotpArapContracts {

    private BotpArapContracts() {
    }

    public record PaymentApplicationCreateRequest(
            @NotBlank String idempotencyKey,
            @NotBlank String sourceSystem,
            @NotBlank String sourceDocumentType,
            @NotBlank String sourceDocumentId,
            @NotBlank String sourceExecutionId,
            @NotBlank String sourceBillNo,
            @NotBlank String counterparty,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String payMethod,
            LocalDate plannedPayDate,
            String operator
    ) {
    }

    public record ArapWritebackRequest(
            @NotNull @DecimalMin(value = "0.00") BigDecimal activeAllocatedAmount,
            @NotBlank String executionId
    ) {
    }
}
