package single.cjj.erp.procurement.contract.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntity;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntryEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseContractContracts {
    private PurchaseContractContracts() {}

    public record CreateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            @NotNull Long fsourcingAwardId,
            String fnumber,
            LocalDate fdate,
            String ftitle,
            LocalDate fstartDate,
            LocalDate fendDate,
            String fpaymentTermCode,
            String fdeliveryTermCode,
            String fremark,
            @NotEmpty List<@Valid EntryRequest> entries
    ) {}

    public record UpdateRequest(
            @NotBlank String ftenantId,
            @NotNull Long forgId,
            String ftitle,
            LocalDate fstartDate,
            LocalDate fendDate,
            String fpaymentTermCode,
            String fdeliveryTermCode,
            String fremark,
            @NotEmpty List<@Valid EntryRequest> entries
    ) {}

    public record EntryRequest(
            @NotNull Long fsourcingAwardEntryId,
            @NotNull @DecimalMin("0.000001") BigDecimal fquantity,
            LocalDate fplannedDeliveryDate
    ) {}

    public record ApprovalResultRequest(
            @NotBlank String status,
            String workflowInstanceId,
            Long operatorId,
            String reason
    ) {}

    public record Detail(
            PurchaseContractEntity contract,
            List<PurchaseContractEntryEntity> entries
    ) {}
}
