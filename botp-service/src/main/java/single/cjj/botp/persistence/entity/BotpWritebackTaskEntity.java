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
@TableName("matrix_botp_writeback_task")
public class BotpWritebackTaskEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private String fexecutionId;
    private Long frelationId;
    private String fsourceSystemCode;
    private String fsourceDocumentType;
    private String fsourceDocumentId;
    private String ftargetSystemCode;
    private String ftargetDocumentType;
    private String ftargetDocumentId;
    private String ftargetDocumentNo;
    private String ftaskType;
    private String fstatus;
    private BigDecimal factiveAllocatedAmount;
    private BigDecimal freleaseReservedAmount;
    private String fcommandJson;
    private Integer fretryCount;
    private LocalDateTime fnextRetryTime;
    private String ferrorMessage;
    private LocalDateTime ffinishTime;
    private LocalDateTime fcreateTime;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
