package single.cjj.botp.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("matrix_botp_reconciliation_issue")
public class BotpReconciliationIssueEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private String fissueType;
    private String fstatus;
    private String fexecutionId;
    private Long frelationId;
    private String fsourceSystemCode;
    private String fsourceDocumentType;
    private String fsourceDocumentId;
    private String ftargetSystemCode;
    private String ftargetDocumentType;
    private String ftargetDocumentId;
    private String ftargetDocumentNo;
    private BigDecimal fexpectedAmount;
    private BigDecimal factualAmount;
    private String fdescription;
    private String fresolution;
    private LocalDateTime fdetectedTime;
    private LocalDateTime fresolvedTime;
    private LocalDateTime fcreateTime;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
