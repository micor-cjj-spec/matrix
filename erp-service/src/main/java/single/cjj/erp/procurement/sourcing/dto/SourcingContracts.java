package single.cjj.erp.procurement.sourcing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import single.cjj.erp.procurement.sourcing.entity.ProcurementRfqEntity;
import single.cjj.erp.procurement.sourcing.entity.ProcurementRfqEntryEntity;
import single.cjj.erp.procurement.sourcing.entity.ProcurementRfqSupplierEntity;
import single.cjj.erp.procurement.sourcing.entity.SourcingAwardEntity;
import single.cjj.erp.procurement.sourcing.entity.SourcingAwardEntryEntity;
import single.cjj.erp.procurement.sourcing.entity.SupplierQuoteEntity;
import single.cjj.erp.procurement.sourcing.entity.SupplierQuoteEntryEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SourcingContracts {
    private SourcingContracts() {}

    public record RfqCreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            String fnumber,
            LocalDate fdate,
            String ftitle,
            @NotBlank String fcurrencyCode,
            LocalDateTime fquotationDeadline,
            String fremark,
            @NotEmpty List<@Valid RfqEntryRequest> entries,
            @NotEmpty List<@Valid RfqSupplierRequest> suppliers) {}

    public record RfqEntryRequest(
            @NotNull Long fpurchaseRequestId,
            @NotNull Long fpurchaseRequestEntryId,
            @NotNull @DecimalMin("0.000001") BigDecimal fquantity) {}

    public record RfqSupplierRequest(
            @NotNull Long fbusinessPartnerId,
            @NotBlank String fbusinessPartnerCode,
            @NotBlank String fbusinessPartnerName) {}

    public record RfqDetail(
            ProcurementRfqEntity rfq,
            List<ProcurementRfqEntryEntity> entries,
            List<ProcurementRfqSupplierEntity> suppliers) {}

    public record QuoteCreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long fbusinessPartnerId,
            String fquoteNo,
            LocalDate fquoteDate,
            LocalDate fvalidUntil,
            Integer fdeliveryDays,
            String fpaymentTerms,
            String fremark,
            @NotEmpty List<@Valid QuoteEntryRequest> entries) {}

    public record QuoteEntryRequest(
            @NotNull Long frfqEntryId,
            @NotNull @DecimalMin("0.000001") BigDecimal fquantity,
            @NotNull @DecimalMin("0.00") BigDecimal funitPrice,
            @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal ftaxRate,
            LocalDate fdeliveryDate,
            String fremark) {}

    public record QuoteDetail(
            SupplierQuoteEntity quote,
            List<SupplierQuoteEntryEntity> entries) {}

    public record ComparisonLine(
            Long rfqEntryId,
            Long quoteId,
            Long quoteEntryId,
            Long businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal grossUnitPrice,
            BigDecimal grossAmount,
            LocalDate deliveryDate,
            boolean lowestGrossUnitPrice) {}

    public record AwardCreateRequest(
            @NotBlank String ftenantId,
            String fnumber,
            LocalDate fdate,
            String fremark,
            @NotEmpty List<@Valid AwardEntryRequest> entries) {}

    public record AwardEntryRequest(
            @NotNull Long frfqEntryId,
            @NotNull Long fquoteId,
            @NotNull Long fquoteEntryId,
            @NotNull @DecimalMin("0.000001") BigDecimal fawardedQuantity,
            String freason) {}

    public record AwardDetail(
            SourcingAwardEntity award,
            List<SourcingAwardEntryEntity> entries) {}
}
