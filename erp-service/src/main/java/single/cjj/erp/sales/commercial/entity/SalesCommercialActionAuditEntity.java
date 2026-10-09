package single.cjj.erp.sales.commercial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/** Append-only business action history. No update or delete API exists. */
@Data
@TableName("matrix_erp_sales_action_audit")
public class SalesCommercialActionAuditEntity {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fid;
    private String ftenantId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long forgId;
    private String fdocumentType;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fdocumentId;
    private String faction;
    private String fbeforeStatus;
    private String fafterStatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long foperatorId;
    private LocalDateTime fcreateTime;
}
