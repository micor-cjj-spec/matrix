package single.cjj.erp.procurement.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_purchase_order_entry")
public class PurchaseOrderEntryEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private Long fpurchaseOrderId;
    private Integer flineNo;
    private Long fmaterialId;
    private String fmaterialCode;
    private String fmaterialName;
    private String fspecification;
    private Long funitId;
    private BigDecimal fquantity;
    private BigDecimal funitPrice;
    private BigDecimal fnetAmount;
    private BigDecimal ftaxRate;
    private BigDecimal ftaxAmount;
    private BigDecimal fgrossAmount;
    private LocalDate fplannedDeliveryDate;
    private Long fprojectId;
    private Long fcostCenterId;
    private BigDecimal freceivedQuantity;
    private BigDecimal facceptedQuantity;
    private BigDecimal finboundQuantity;
    private BigDecimal finvoicedQuantity;
    private BigDecimal fsettledAmount;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
