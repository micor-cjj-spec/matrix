package single.cjj.erp.procurement.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_supplier_delivery_response_entry")
public class SupplierDeliveryResponseEntryEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private Long fresponseId;
    private Integer flineNo;
    private Long fdeliveryPlanEntryId;
    private BigDecimal fcommittedQuantity;
    private LocalDate fcommittedDeliveryDate;
    private String freason;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
