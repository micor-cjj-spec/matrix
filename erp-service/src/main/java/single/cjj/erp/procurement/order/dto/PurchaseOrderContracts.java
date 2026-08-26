package single.cjj.erp.procurement.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseOrderContracts {

    private PurchaseOrderContracts() {
    }

    public record PurchaseOrderCreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            String fnumber,
            LocalDate fdate,
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fbusinessPartnerCode,
            @NotBlank String fbusinessPartnerName,
            Long fcontractId,
            Long fcurrencyId,
            @NotBlank String fcurrencyCode,
            String fpaymentTermCode,
            LocalDate fplannedDeliveryDate,
            @NotEmpty List<@Valid PurchaseOrderEntryRequest> entries
    ) {
    }

    public record PurchaseOrderUpdateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fbusinessPartnerCode,
            @NotBlank String fbusinessPartnerName,
            Long fcontractId,
            Long fcurrencyId,
            @NotBlank String fcurrencyCode,
            String fpaymentTermCode,
            LocalDate fplannedDeliveryDate,
            @NotEmpty List<@Valid PurchaseOrderEntryRequest> entries
    ) {
    }

    public record PurchaseOrderEntryRequest(
            @NotNull Long fmaterialId,
            @NotBlank String fmaterialCode,
            @NotBlank String fmaterialName,
            String fspecification,
            Long funitId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal fquantity,
            @NotNull @DecimalMin(value = "0.00") BigDecimal funitPrice,
            @DecimalMin(value = "0.00") @DecimalMax(value = "1.00") BigDecimal ftaxRate,
            LocalDate fplannedDeliveryDate,
            Long fprojectId,
            Long fcostCenterId
    ) {
    }

    public record PurchaseOrderDetail(
            PurchaseOrderEntity order,
            List<PurchaseOrderEntryEntity> entries
    ) {
    }
}
