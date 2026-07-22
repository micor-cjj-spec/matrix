package single.cjj.scheduler.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_scheduler_outbox")
public class MatrixSchedulerOutbox {

    @TableId
    private Long fid;
    private String feventId;
    private String feventType;
    private String faggregateId;
    private String froutingKey;
    private String fpayload;
    private String fstatus;
    private Integer fretryCount;
    private LocalDateTime fnextRetryTime;
    private String flastError;
    private LocalDateTime fcreateTime;
    private LocalDateTime fupdateTime;
}
