package single.cjj.erp.procurement.sourcing.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.*;
@Data
@TableName("matrix_erp_supplier_quote")
public class SupplierQuoteEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private Long frfqId; private Long fbusinessPartnerId;
 private String fbusinessPartnerCode; private String fbusinessPartnerName; private String fquoteNo; private LocalDate fquoteDate;
 private LocalDate fvalidUntil; private String fcurrencyCode; private Integer fdeliveryDays; private String fpaymentTerms;
 private BigDecimal fnetAmount; private BigDecimal ftaxAmount; private BigDecimal fgrossAmount; private String fstatus;
 private LocalDateTime fsubmittedTime; private String fremark; private Long fcreateBy; private LocalDateTime fcreateTime;
 private Long fmodifyBy; private LocalDateTime fmodifyTime; @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
