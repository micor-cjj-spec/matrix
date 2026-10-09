package single.cjj.scheduler.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_scheduler_job")
public class MatrixSchedulerJob {

    @TableId
    private Long fid;
    private String fjobCode;
    private String fjobName;
    private String fsourceType;
    private String fsourceService;
    private String ftenantId;
    private String fscheduleType;
    private String fcronExpression;
    private String ftimezone;
    private String fexecuteType;
    private String fexecutorCode;
    private String fhandlerCode;
    private String fexecuteParameters;
    private String fstatus;
    private String fconcurrencyPolicy;
    private String fmisfirePolicy;
    private Integer ftimeoutSeconds;
    private Integer fretryCount;
    private Integer fretryIntervalSeconds;
    private LocalDateTime fnextFireTime;
    private LocalDateTime flastFireTime;
    private String fidempotencyKey;
    private Integer fversion;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fupdateBy;
    private LocalDateTime fupdateTime;
}
