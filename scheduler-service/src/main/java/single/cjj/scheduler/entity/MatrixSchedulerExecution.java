package single.cjj.scheduler.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_scheduler_execution")
public class MatrixSchedulerExecution {

    @TableId
    private Long fid;
    private String fexecutionNo;
    private Long fjobId;
    private String fjobCode;
    private LocalDateTime fscheduledTime;
    private LocalDateTime factualStartTime;
    private LocalDateTime factualEndTime;
    private String ftriggerType;
    private String fstatus;
    private Integer fattemptNo;
    private Long frootExecutionId;
    private Long fparentExecutionId;
    private LocalDateTime fnextRetryTime;
    private LocalDateTime fdeadlineTime;
    private String fexecutorCode;
    private String fhandlerCode;
    private String fexecutorInstance;
    private String frequestPayload;
    private String fresponsePayload;
    private String ferrorCode;
    private String ferrorMessage;
    private String ftraceId;
    private String fidempotencyKey;
    private LocalDateTime fcreateTime;
    private LocalDateTime fupdateTime;
}
