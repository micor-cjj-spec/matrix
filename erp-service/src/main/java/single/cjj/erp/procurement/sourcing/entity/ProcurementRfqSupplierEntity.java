package single.cjj.erp.procurement.sourcing.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data
@TableName("matrix_erp_procurement_rfq_supplier")
public class ProcurementRfqSupplierEntity {
 @TableId(type=IdType.ASSIGN_ID) private Long fid;
 private String ftenantId; private Long forgId; private Long frfqId; private Long fbusinessPartnerId;
 private String fbusinessPartnerCode; private String fbusinessPartnerName; private String fstatus;
 private Long fcreateBy; private LocalDateTime fcreateTime; private Long fmodifyBy; private LocalDateTime fmodifyTime;
 @TableLogic private Integer fdeleteFlag; @Version private Integer fversion;
}
