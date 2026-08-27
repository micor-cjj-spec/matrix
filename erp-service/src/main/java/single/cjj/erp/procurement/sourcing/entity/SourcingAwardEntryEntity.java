package single.cjj.erp.procurement.sourcing.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data
@TableName("matrix_erp_sourcing_award_entry")
public class SourcingAwardEntryEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private Long fawardId; private Integer flineNo; private Long frfqEntryId;
 private Long fquoteId; private Long fquoteEntryId; private Long fbusinessPartnerId; private String fbusinessPartnerCode;
 private String fbusinessPartnerName; private BigDecimal fawardedQuantity; private BigDecimal funitPrice; private BigDecimal ftaxRate;
 private BigDecimal fnetAmount; private BigDecimal ftaxAmount; private BigDecimal fgrossAmount; private String freason;
 private Long fcreateBy; private LocalDateTime fcreateTime; private Long fmodifyBy; private LocalDateTime fmodifyTime;
 @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
