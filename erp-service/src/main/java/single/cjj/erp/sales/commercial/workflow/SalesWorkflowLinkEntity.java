package single.cjj.erp.sales.commercial.workflow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_sales_workflow_link")
public class SalesWorkflowLinkEntity {
    @TableId(type=IdType.INPUT)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String fdocumentType;
    private Long fdocumentId;
    private String fdefinitionKey;
    private Long finitiatorId;
    private String fidempotencyKey;
    private String finstanceId;
    private String fstatus;
    private Integer fretryCount;
    private LocalDateTime fnextRetryTime;
    private String fprocessedEventId;
    private LocalDateTime fcreateTime;
    private LocalDateTime fmodifyTime;
}
