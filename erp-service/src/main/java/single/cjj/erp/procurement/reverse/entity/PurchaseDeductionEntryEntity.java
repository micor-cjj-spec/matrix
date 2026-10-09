package single.cjj.erp.procurement.reverse.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("matrix_erp_purchase_deduction_entry")
public class PurchaseDeductionEntryEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private Long fpurchaseDeductionId; private Integer flineNo; private Long fsupplierClaimEntryId;
 private Long fpurchaseOrderId; private Long fpurchaseOrderEntryId; private Long fmaterialId; private String fmaterialCode; private String fmaterialName;
 private BigDecimal famount; private Long fcreateBy; private LocalDateTime fcreateTime; private Long fmodifyBy; private LocalDateTime fmodifyTime;
 @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
