package single.cjj.erp.procurement.sourcing.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.*;
@Data
@TableName("matrix_erp_sourcing_award")
public class SourcingAwardEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private Long frfqId; private String fnumber; private LocalDate fdate;
 private BigDecimal fgrossAmount; private String fstatus; private String fremark; private Long fcreateBy; private LocalDateTime fcreateTime;
 private Long fmodifyBy; private LocalDateTime fmodifyTime; @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
