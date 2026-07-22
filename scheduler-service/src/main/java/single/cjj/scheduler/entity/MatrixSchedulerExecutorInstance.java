package single.cjj.scheduler.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_scheduler_executor_instance")
public class MatrixSchedulerExecutorInstance {

    @TableId
    private Long fid;
    private String fexecutorCode;
    private String finstanceId;
    private String fstatus;
    private Integer fmaxConcurrency;
    private Integer frunningCount;
    private LocalDateTime flastHeartbeatTime;
    private LocalDateTime fcreateTime;
    private LocalDateTime fupdateTime;
}
