package single.cjj.scheduler.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_scheduler_handler")
public class MatrixSchedulerHandler {

    @TableId
    private Long fid;
    private String fexecutorCode;
    private String fhandlerCode;
    private String fhandlerName;
    private String fstatus;
    private LocalDateTime fcreateTime;
    private LocalDateTime fupdateTime;
}
