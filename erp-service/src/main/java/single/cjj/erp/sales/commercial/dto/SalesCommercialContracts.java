package single.cjj.erp.sales.commercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import single.cjj.erp.sales.commercial.entity.*;

public final class SalesCommercialContracts {
    private SalesCommercialContracts() {}

    public record QuoteLine(
            @NotBlank String fdescription,
            String fmaterialCode,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal fquantity,
            @NotNull @DecimalMin("0") BigDecimal funitPrice,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal ftaxRate) {}

    public record CreateQuote(
            @NotBlank String ftenantId,
            Long forgId,
            String fnumber,
            @NotNull Long fopportunityId,
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fcurrencyCode,
            String fquoteType,
            String ftenderReference,
            @NotNull LocalDate fvalidUntil,
            String fdeliveryTermCode,
            String fpaymentTermCode,
            @NotEmpty List<@Valid QuoteLine> entries) {}

    public record CreateContract(
            @NotBlank String ftenantId,
            String fnumber,
            @NotNull Long fquoteId,
            @NotBlank String ftitle,
            @NotNull LocalDate fstartDate,
            @NotNull LocalDate fendDate) {}

    public record QuoteDetail(SalesQuoteEntity header, List<SalesQuoteEntryEntity> entries) {}
    public record ContractDetail(SalesContractEntity header, List<SalesContractEntryEntity> entries) {}
}

