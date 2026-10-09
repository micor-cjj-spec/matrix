package single.cjj.erp.procurement.receipt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import single.cjj.erp.procurement.receipt.entity.PurchaseReceiptEntity;
import single.cjj.erp.procurement.receipt.entity.PurchaseReceiptEntryEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseReceiptContracts {

    private PurchaseReceiptContracts() {
    }

    public record PurchaseReceiptCreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            String fnumber,
            LocalDate fdate,
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fbusinessPartnerCode,
            @NotBlank String fbusinessPartnerName,
            @NotBlank String fcurrencyCode,
            String fsupplierDeliveryNo,
            Long fwarehouseId,
            String fbotpIdempotencyKey,
            String fsourceExecutionId,
            @NotEmpty List<@Valid PurchaseReceiptEntryRequest> entries
    ) {
    }

    public record PurchaseReceiptUpdateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fbusinessPartnerCode,
            @NotBlank String fbusinessPartnerName,
            @NotBlank String fcurrencyCode,
            String fsupplierDeliveryNo,
            Long fwarehouseId,
            @NotEmpty List<@Valid PurchaseReceiptEntryRequest> entries
    ) {
    }

    public record PurchaseReceiptEntryRequest(
            @NotNull Long fpurchaseOrderEntryId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal fquantity,
            String fbatchNo,
            Long fwarehouseId
    ) {
    }

    public record PurchaseReceiptDetail(
            PurchaseReceiptEntity receipt,
            List<PurchaseReceiptEntryEntity> entries
    ) {
    }
}
