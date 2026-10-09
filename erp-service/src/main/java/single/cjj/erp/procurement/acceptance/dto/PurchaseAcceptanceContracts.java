package single.cjj.erp.procurement.acceptance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import single.cjj.erp.procurement.acceptance.entity.PurchaseAcceptanceEntity;
import single.cjj.erp.procurement.acceptance.entity.PurchaseAcceptanceEntryEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseAcceptanceContracts {

    private PurchaseAcceptanceContracts() {
    }

    public record PurchaseAcceptanceCreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            String fnumber,
            LocalDate fdate,
            @NotNull Long fpurchaseReceiptId,
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fbusinessPartnerCode,
            @NotBlank String fbusinessPartnerName,
            @NotBlank String fcurrencyCode,
            String fbotpIdempotencyKey,
            String fsourceExecutionId,
            @NotEmpty List<@Valid PurchaseAcceptanceEntryRequest> entries
    ) {
    }

    public record PurchaseAcceptanceUpdateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            @NotEmpty List<@Valid PurchaseAcceptanceEntryRequest> entries
    ) {
    }

    public record PurchaseAcceptanceEntryRequest(
            @NotNull Long fpurchaseReceiptEntryId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal finspectionQuantity,
            @DecimalMin(value = "0") BigDecimal fqualifiedQuantity,
            @DecimalMin(value = "0") BigDecimal fconcessionQuantity,
            @DecimalMin(value = "0") BigDecimal frejectedQuantity,
            String finspectionMethod,
            String fqualityResult
    ) {
    }

    public record PurchaseAcceptanceDetail(
            PurchaseAcceptanceEntity acceptance,
            List<PurchaseAcceptanceEntryEntity> entries
    ) {
    }
}
