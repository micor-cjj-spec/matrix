package single.cjj.erp.crm.lead.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class CrmLeadContracts {

    private CrmLeadContracts() {
    }

    public record CreateRequest(
            @NotBlank String ftenantId,
            Long forgId,
            String fnumber,
            LocalDate fdate,
            @NotBlank String fname,
            String fcompanyName,
            String fcontactName,
            String fcontactPhone,
            String fcontactEmail,
            String fsource,
            Long fownerId,
            @DecimalMin("0") BigDecimal festimatedAmount,
            String fcurrencyCode,
            LocalDate fnextActionDate
    ) {
    }

    public record UpdateRequest(
            @NotBlank String ftenantId,
            Long forgId,
            @NotBlank String fname,
            String fcompanyName,
            String fcontactName,
            String fcontactPhone,
            String fcontactEmail,
            String fsource,
            Long fownerId,
            @DecimalMin("0") BigDecimal festimatedAmount,
            String fcurrencyCode,
            LocalDate fnextActionDate
    ) {
    }

    public record DisqualifyRequest(
            @NotBlank String reason
    ) {
    }
}
