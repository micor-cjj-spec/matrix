package single.cjj.erp.procurement.reverse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import single.cjj.erp.procurement.reverse.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseReverseContracts {
    private PurchaseReverseContracts() {}

    public record ReturnCreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long fpurchaseInboundId,
            String fnumber,
            LocalDate fdate,
            @NotBlank String freasonType,
            String freason,
            @NotEmpty List<@Valid ReturnEntryRequest> entries) {}

    public record ReturnEntryRequest(
            @NotNull Long fpurchaseInboundEntryId,
            @NotNull @DecimalMin("0.000001") BigDecimal fquantity) {}

    public record ReturnDetail(
            PurchaseReturnEntity header,
            List<PurchaseReturnEntryEntity> entries) {}

    public record ClaimCreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long fpurchaseOrderId,
            Long fpurchaseReturnId,
            String fnumber,
            LocalDate fdate,
            @NotBlank String fclaimType,
            String freason,
            @NotEmpty List<@Valid ClaimEntryRequest> entries) {}

    public record ClaimEntryRequest(
            @NotNull Long fpurchaseOrderEntryId,
            Long fpurchaseReturnEntryId,
            @NotNull @DecimalMin("0.01") BigDecimal frequestedAmount,
            String freason) {}

    public record ClaimConfirmRequest(
            @NotBlank String ftenantId,
            @NotEmpty List<@Valid ClaimAgreeEntryRequest> entries) {}

    public record ClaimAgreeEntryRequest(
            @NotNull Long fsupplierClaimEntryId,
            @NotNull @DecimalMin("0.00") BigDecimal fagreedAmount) {}

    public record ClaimDetail(
            SupplierClaimEntity header,
            List<SupplierClaimEntryEntity> entries) {}

    public record DeductionCreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long fsupplierClaimId,
            String fnumber,
            LocalDate fdate,
            String freason,
            @NotEmpty List<@Valid DeductionEntryRequest> entries) {}

    public record DeductionEntryRequest(
            @NotNull Long fsupplierClaimEntryId,
            @NotNull @DecimalMin("0.01") BigDecimal famount) {}

    public record DeductionDetail(
            PurchaseDeductionEntity header,
            List<PurchaseDeductionEntryEntity> entries) {}
}
