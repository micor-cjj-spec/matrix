package single.cjj.scheduler.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_scheduler_operation_log")
public class MatrixSchedulerOperationLog {

    @TableId
    private Long fid;
    private String fexecutionNo;
    private String faction;
    private String foperatorId;
    private String freason;
    private String ffromStatus;
    private String ftoStatus;
    private LocalDateTime fcreateTime;
}
