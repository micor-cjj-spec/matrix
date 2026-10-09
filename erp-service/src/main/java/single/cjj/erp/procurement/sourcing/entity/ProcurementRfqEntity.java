package single.cjj.erp.procurement.sourcing.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;
@Data
@TableName("matrix_erp_procurement_rfq")
public class ProcurementRfqEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private String fnumber; private LocalDate fdate;
 private String ftitle; private String fcurrencyCode; private LocalDateTime fquotationDeadline; private String fstatus;
 private LocalDateTime fpublishedTime; private LocalDateTime fclosedTime; private String fremark;
 private Long fcreateBy; private LocalDateTime fcreateTime; private Long fmodifyBy; private LocalDateTime fmodifyTime;
 @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
