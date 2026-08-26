package single.cjj.erp.procurement.inbound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_purchase_inbound_entry")
public class PurchaseInboundEntryEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private Long fpurchaseInboundId;
    private Integer flineNo;
    private Long fpurchaseAcceptanceId;
    private Long fpurchaseAcceptanceEntryId;
    private Long fpurchaseReceiptEntryId;
    private Long fpurchaseOrderId;
    private Long fpurchaseOrderEntryId;
    private Long fmaterialId;
    private String fmaterialCode;
    private String fmaterialName;
    private String fspecification;
    private Long funitId;
    private BigDecimal fquantity;
    private BigDecimal funitPrice;
    private BigDecimal famount;
    private String fbatchNo;
    private Long fwarehouseId;
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
