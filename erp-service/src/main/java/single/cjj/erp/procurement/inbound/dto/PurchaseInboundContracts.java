package single.cjj.erp.procurement.inbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntity;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntryEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseInboundContracts {

    private PurchaseInboundContracts() {
    }

    public record PurchaseInboundCreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            String fnumber,
            LocalDate fdate,
            @NotNull Long fpurchaseAcceptanceId,
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fbusinessPartnerCode,
            @NotBlank String fbusinessPartnerName,
            @NotBlank String fcurrencyCode,
            Long fwarehouseId,
            String fbotpIdempotencyKey,
            String fsourceExecutionId,
            @NotEmpty List<@Valid PurchaseInboundEntryRequest> entries
    ) {
    }

    public record PurchaseInboundUpdateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            Long fwarehouseId,
            @NotEmpty List<@Valid PurchaseInboundEntryRequest> entries
    ) {
    }

    public record PurchaseInboundEntryRequest(
            @NotNull Long fpurchaseAcceptanceEntryId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal fquantity,
            String fbatchNo,
            Long fwarehouseId
    ) {
    }

    public record PurchaseInboundDetail(
            PurchaseInboundEntity inbound,
            List<PurchaseInboundEntryEntity> entries
    ) {
    }
}
