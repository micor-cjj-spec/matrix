package single.cjj.botp.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_botp_execution_log")
public class BotpExecutionLogEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String fexecutionId;
    private String fstage;
    private String fstatus;
    private String fmessage;
    private String frequestSnapshot;
    private String fresponseSnapshot;
    private String fexceptionType;
    private LocalDateTime fstartTime;
    private LocalDateTime ffinishTime;
    private LocalDateTime fcreateTime;
    @TableLogic
    private Integer fdeleteFlag;
}
