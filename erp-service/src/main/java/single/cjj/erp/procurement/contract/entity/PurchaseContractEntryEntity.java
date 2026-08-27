package single.cjj.erp.procurement.contract.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_purchase_contract_entry")
public class PurchaseContractEntryEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private Long fpurchaseContractId;
    private Integer flineNo;
    private Long fsourcingAwardEntryId;
    private Long frfqEntryId;
    private Long fpurchaseRequestId;
    private Long fpurchaseRequestEntryId;
    private Long fmaterialId;
    private String fmaterialCode;
    private String fmaterialName;
    private String fspecification;
    private Long funitId;
    private BigDecimal fquantity;
    private BigDecimal funitPrice;
    private BigDecimal ftaxRate;
    private BigDecimal fnetAmount;
    private BigDecimal ftaxAmount;
    private BigDecimal fgrossAmount;
    private LocalDate fplannedDeliveryDate;
    private Long fprojectId;
    private Long fcostCenterId;
    private BigDecimal forderedQuantity;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
