package single.cjj.erp.procurement.sourcing.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.*;
@Data
@TableName("matrix_erp_supplier_quote_entry")
public class SupplierQuoteEntryEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private Long fquoteId; private Long frfqEntryId; private Integer flineNo;
 private BigDecimal fquantity; private BigDecimal fawardedQuantity; private BigDecimal funitPrice; private BigDecimal ftaxRate;
 private BigDecimal fnetAmount; private BigDecimal ftaxAmount; private BigDecimal fgrossAmount; private LocalDate fdeliveryDate;
 private String fremark; private Long fcreateBy; private LocalDateTime fcreateTime; private Long fmodifyBy; private LocalDateTime fmodifyTime;
 @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
