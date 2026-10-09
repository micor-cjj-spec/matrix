package single.cjj.erp.procurement.reverse.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.*;
@Data @TableName("matrix_erp_purchase_return")
public class PurchaseReturnEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private String fnumber; private LocalDate fdate;
 private Long fpurchaseInboundId; private Long fpurchaseOrderId;
 private Long fbusinessPartnerId; private String fbusinessPartnerCode; private String fbusinessPartnerName;
 private String fcurrencyCode; private Long fwarehouseId; private BigDecimal ftotalQuantity; private BigDecimal ftotalAmount;
 private String freasonType; private String freason; private String fstatus; private String fapprovalStatus;
 private Long fcreateBy; private LocalDateTime fcreateTime; private Long fmodifyBy; private LocalDateTime fmodifyTime;
 @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
