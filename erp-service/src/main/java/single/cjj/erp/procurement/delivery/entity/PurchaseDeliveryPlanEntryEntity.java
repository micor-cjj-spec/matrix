package single.cjj.erp.procurement.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_purchase_delivery_plan_entry")
public class PurchaseDeliveryPlanEntryEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private Long fdeliveryPlanId;
    private Integer flineNo;
    private Long fpurchaseOrderId;
    private Long fpurchaseOrderEntryId;
    private Long fmaterialId;
    private String fmaterialCode;
    private String fmaterialName;
    private String fspecification;
    private Long funitId;
    private BigDecimal fplannedQuantity;
    private LocalDate fplannedDeliveryDate;
    private BigDecimal fcommittedQuantity;
    private LocalDate fcommittedDeliveryDate;
    private String fresponseStatus;
    private BigDecimal freceivedQuantity;
    private Long flatestResponseEntryId;
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
