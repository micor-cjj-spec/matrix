package single.cjj.erp.crm.opportunity.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class CrmOpportunityContracts {

    private CrmOpportunityContracts() {
    }

    public record CreateRequest(
            @NotBlank String ftenantId,
            Long forgId,
            String fnumber,
            LocalDate fdate,
            Long fleadId,
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fname,
            Long fownerId,
            @NotBlank String fcurrencyCode,
            @NotNull @DecimalMin("0") BigDecimal fexpectedAmount,
            LocalDate fexpectedCloseDate,
            String fstage,
            @DecimalMin("0") @DecimalMax("100") BigDecimal fprobability,
            LocalDate fnextActionDate
    ) {
    }

    public record UpdateRequest(
            @NotBlank String ftenantId,
            Long forgId,
            @NotBlank String fname,
            Long fownerId,
            @NotBlank String fcurrencyCode,
            @NotNull @DecimalMin("0") BigDecimal fexpectedAmount,
            LocalDate fexpectedCloseDate,
            LocalDate fnextActionDate
    ) {
    }

    public record StageRequest(
            @NotBlank String stage,
            @DecimalMin("0") @DecimalMax("100") BigDecimal probability,
            LocalDate nextActionDate
    ) {
    }

    public record LoseRequest(
            @NotBlank String reason
    ) {
    }
}
