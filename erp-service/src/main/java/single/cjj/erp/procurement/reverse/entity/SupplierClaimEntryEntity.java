package single.cjj.erp.procurement.reverse.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("matrix_erp_supplier_claim_entry")
public class SupplierClaimEntryEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private Long fsupplierClaimId; private Integer flineNo;
 private Long fpurchaseOrderId; private Long fpurchaseOrderEntryId; private Long fpurchaseReturnEntryId;
 private Long fmaterialId; private String fmaterialCode; private String fmaterialName;
 private BigDecimal frequestedAmount; private BigDecimal fagreedAmount; private BigDecimal fdeductedAmount; private String freason;
 private Long fcreateBy; private LocalDateTime fcreateTime; private Long fmodifyBy; private LocalDateTime fmodifyTime;
 @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
