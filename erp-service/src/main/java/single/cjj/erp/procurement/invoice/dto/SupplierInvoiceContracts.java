package single.cjj.erp.procurement.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import single.cjj.erp.procurement.invoice.entity.SupplierInvoiceEntity;
import single.cjj.erp.procurement.invoice.entity.SupplierInvoiceEntryEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SupplierInvoiceContracts {

    private SupplierInvoiceContracts() {
    }

    public record SupplierInvoiceCreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            String fnumber,
            @NotBlank String finvoiceNo,
            String finvoiceCode,
            @NotNull LocalDate finvoiceDate,
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fbusinessPartnerCode,
            @NotBlank String fbusinessPartnerName,
            @NotBlank String fcurrencyCode,
            @NotEmpty List<@Valid SupplierInvoiceEntryRequest> entries
    ) {
    }

    public record SupplierInvoiceUpdateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            @NotBlank String finvoiceNo,
            String finvoiceCode,
            @NotNull LocalDate finvoiceDate,
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fbusinessPartnerCode,
            @NotBlank String fbusinessPartnerName,
            @NotBlank String fcurrencyCode,
            @NotEmpty List<@Valid SupplierInvoiceEntryRequest> entries
    ) {
    }

    public record SupplierInvoiceEntryRequest(
            @NotNull Long fpurchaseOrderEntryId,
            @NotNull Long fmaterialId,
            @NotBlank String fmaterialCode,
            @NotBlank String fmaterialName,
            String fspecification,
            @NotNull @DecimalMin("0.000001") BigDecimal fquantity,
            @NotNull @DecimalMin("0.00") BigDecimal funitPrice,
            @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal ftaxRate
    ) {
    }

    public record SupplierInvoiceDetail(
            SupplierInvoiceEntity invoice,
            List<SupplierInvoiceEntryEntity> entries
    ) {
    }
}
