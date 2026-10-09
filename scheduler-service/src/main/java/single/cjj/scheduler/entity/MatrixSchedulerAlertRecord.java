package single.cjj.scheduler.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_scheduler_alert_record")
public class MatrixSchedulerAlertRecord {

    @TableId
    private Long fid;
    private String fdedupeKey;
    private String fexecutionNo;
    private Long fjobId;
    private String fexecutorCode;
    private String falertType;
    private String flevel;
    private String ftitle;
    private String fcontent;
    private String fstatus;
    private String fackBy;
    private LocalDateTime fackTime;
    private LocalDateTime fcreateTime;
    private LocalDateTime fupdateTime;
}
