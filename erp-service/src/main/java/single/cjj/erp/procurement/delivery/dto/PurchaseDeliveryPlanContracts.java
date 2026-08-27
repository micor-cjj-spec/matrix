package single.cjj.erp.procurement.delivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import single.cjj.erp.procurement.delivery.entity.PurchaseDeliveryPlanEntity;
import single.cjj.erp.procurement.delivery.entity.PurchaseDeliveryPlanEntryEntity;
import single.cjj.erp.procurement.delivery.entity.SupplierDeliveryResponseEntity;
import single.cjj.erp.procurement.delivery.entity.SupplierDeliveryResponseEntryEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseDeliveryPlanContracts {
    private PurchaseDeliveryPlanContracts() {}

    public record CreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long fpurchaseOrderId,
            String fnumber,
            LocalDate fdate,
            String fremark,
            @NotEmpty List<@Valid EntryRequest> entries
    ) {}

    public record UpdateRequest(
            @NotBlank String ftenantId,
            String fremark,
            @NotEmpty List<@Valid EntryRequest> entries
    ) {}

    public record EntryRequest(
            @NotNull Long fpurchaseOrderEntryId,
            @NotNull @DecimalMin("0.000001") BigDecimal fplannedQuantity,
            @NotNull LocalDate fplannedDeliveryDate
    ) {}

    public record SupplierResponseRequest(
            @NotBlank String ftenantId,
            String fnumber,
            LocalDate fdate,
            @NotBlank String fresponseType,
            String fremark,
            List<@Valid SupplierResponseEntryRequest> entries
    ) {}

    public record SupplierResponseEntryRequest(
            @NotNull Long fdeliveryPlanEntryId,
            @DecimalMin("0.000001") BigDecimal fcommittedQuantity,
            LocalDate fcommittedDeliveryDate,
            String freason
    ) {}

    public record Detail(
            PurchaseDeliveryPlanEntity plan,
            List<PurchaseDeliveryPlanEntryEntity> entries
    ) {}

    public record ResponseDetail(
            SupplierDeliveryResponseEntity response,
            List<SupplierDeliveryResponseEntryEntity> entries
    ) {}
}
