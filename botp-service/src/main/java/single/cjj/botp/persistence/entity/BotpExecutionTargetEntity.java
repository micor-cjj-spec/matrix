package single.cjj.botp.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_botp_execution_target")
public class BotpExecutionTargetEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private String fexecutionId;
    private Integer ftargetIndex;
    private String ftargetIdempotencyKey;
    private String ftargetSystemCode;
    private String ftargetDocumentType;
    private String ftargetDocumentId;
    private String ftargetDocumentNo;
    private String fstatus;
    private String fresponseJson;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
