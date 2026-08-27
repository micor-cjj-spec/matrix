package single.cjj.erp.procurement.sourcing.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.*;
@Data
@TableName("matrix_erp_procurement_rfq_entry")
public class ProcurementRfqEntryEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private Long frfqId; private Integer flineNo;
 private Long fpurchaseRequestId; private Long fpurchaseRequestEntryId; private Long fmaterialId;
 private String fmaterialCode; private String fmaterialName; private String fspecification; private Long funitId;
 private BigDecimal fquantity; private BigDecimal fawardedQuantity; private LocalDate frequiredDate;
 private Long fprojectId; private Long fcostCenterId; private Long fcreateBy; private LocalDateTime fcreateTime;
 private Long fmodifyBy; private LocalDateTime fmodifyTime; @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
