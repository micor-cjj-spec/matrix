package single.cjj.erp.procurement.request.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntity;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntryEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseRequestContracts {
    private PurchaseRequestContracts() {
    }

    public record CreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            String fnumber,
            LocalDate fdate,
            Long frequesterId,
            Long frequestDepartmentId,
            String frequestType,
            String fpurpose,
            @NotBlank String fcurrencyCode,
            @NotNull @DecimalMin("0.00") BigDecimal fbudgetAmount,
            LocalDate frequiredDate,
            Long fprojectId,
            Long fcostCenterId,
            String fsourceDocumentType,
            String fsourceDocumentId,
            String fsourceDocumentNo,
            @NotEmpty List<@Valid EntryRequest> entries
    ) {
    }

    public record UpdateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            Long frequesterId,
            Long frequestDepartmentId,
            String frequestType,
            String fpurpose,
            @NotBlank String fcurrencyCode,
            @NotNull @DecimalMin("0.00") BigDecimal fbudgetAmount,
            LocalDate frequiredDate,
            Long fprojectId,
            Long fcostCenterId,
            String fsourceDocumentType,
            String fsourceDocumentId,
            String fsourceDocumentNo,
            @NotEmpty List<@Valid EntryRequest> entries
    ) {
    }

    public record EntryRequest(
            @NotNull Long fmaterialId,
            @NotBlank String fmaterialCode,
            @NotBlank String fmaterialName,
            String fspecification,
            Long funitId,
            @NotNull @DecimalMin("0.000001") BigDecimal fquantity,
            @NotNull @DecimalMin("0.00") BigDecimal festimatedUnitPrice,
            LocalDate frequiredDate,
            Long fprojectId,
            Long fcostCenterId
    ) {
    }

    public record ApprovalResultRequest(
            @NotBlank String status,
            String workflowInstanceId,
            Long operatorId,
            String reason
    ) {
    }

    public record Detail(
            PurchaseRequestEntity request,
            List<PurchaseRequestEntryEntity> entries
    ) {
    }
}
