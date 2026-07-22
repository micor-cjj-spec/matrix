package single.cjj.scheduler.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_scheduler_executor")
public class MatrixSchedulerExecutor {

    @TableId
    private Long fid;
    private String fexecutorCode;
    private String fexecutorName;
    private String fexecuteType;
    private String fserviceName;
    private String fbaseUrl;
    private String fauthType;
    private String fsecretRef;
    private String fstatus;
    private LocalDateTime flastHeartbeatTime;
    private LocalDateTime fcreateTime;
    private LocalDateTime fupdateTime;
}
