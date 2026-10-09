package single.cjj.erp.procurement.reverse.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("matrix_erp_purchase_return_entry")
public class PurchaseReturnEntryEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private Long fpurchaseReturnId; private Integer flineNo;
 private Long fpurchaseInboundId; private Long fpurchaseInboundEntryId; private Long fpurchaseOrderId; private Long fpurchaseOrderEntryId;
 private Long fmaterialId; private String fmaterialCode; private String fmaterialName; private String fspecification; private Long funitId;
 private BigDecimal fquantity; private BigDecimal funitPrice; private BigDecimal famount; private String fbatchNo; private Long fwarehouseId;
 private Long fprojectId; private Long fcostCenterId; private Long fcreateBy; private LocalDateTime fcreateTime;
 private Long fmodifyBy; private LocalDateTime fmodifyTime; @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
