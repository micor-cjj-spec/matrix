package single.cjj.erp.procurement.receipt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_purchase_receipt_entry")
public class PurchaseReceiptEntryEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private Long fpurchaseReceiptId;
    private Integer flineNo;
    private Long fpurchaseOrderId;
    private Long fpurchaseOrderEntryId;
    private Long fmaterialId;
    private String fmaterialCode;
    private String fmaterialName;
    private String fspecification;
    private Long funitId;
    private BigDecimal fquantity;
    private String fbatchNo;
    private Long fwarehouseId;
    private BigDecimal finspectionReservedQuantity;
    private BigDecimal finspectedQuantity;
    private Long fprojectId;
    private Long fcostCenterId;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
