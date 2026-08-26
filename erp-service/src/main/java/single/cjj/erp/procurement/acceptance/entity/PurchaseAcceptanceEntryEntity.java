package single.cjj.erp.procurement.acceptance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_purchase_acceptance_entry")
public class PurchaseAcceptanceEntryEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private Long fpurchaseAcceptanceId;
    private Integer flineNo;
    private Long fpurchaseReceiptId;
    private Long fpurchaseReceiptEntryId;
    private Long fpurchaseOrderId;
    private Long fpurchaseOrderEntryId;
    private Long fmaterialId;
    private String fmaterialCode;
    private String fmaterialName;
    private String fspecification;
    private Long funitId;
    private BigDecimal finspectionQuantity;
    private BigDecimal fqualifiedQuantity;
    private BigDecimal fconcessionQuantity;
    private BigDecimal frejectedQuantity;
    private String finspectionMethod;
    private String fqualityResult;
    private String fbatchNo;
    private BigDecimal funitPrice;
    private Long fprojectId;
    private Long fcostCenterId;
    private BigDecimal finboundReservedQuantity;
    private BigDecimal finboundQuantity;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
