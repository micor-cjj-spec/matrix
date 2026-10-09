package single.cjj.erp.procurement.reverse.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.*;
@Data @TableName("matrix_erp_purchase_deduction")
public class PurchaseDeductionEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private String fnumber; private LocalDate fdate; private Long fsupplierClaimId;
 private Long fpurchaseOrderId; private Long fbusinessPartnerId; private String fbusinessPartnerCode; private String fbusinessPartnerName;
 private String fcurrencyCode; private BigDecimal famount; private String freason; private String fstatus; private String fapprovalStatus;
 private Long fcreateBy; private LocalDateTime fcreateTime; private Long fmodifyBy; private LocalDateTime fmodifyTime;
 @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
