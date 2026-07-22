package single.cjj.scheduler.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_scheduler_im_outbox")
public class MatrixSchedulerImOutbox {

    @TableId
    private Long fid;
    private Long falertId;
    private String frequestId;
    private String fpayload;
    private String fstatus;
    private Integer fretryCount;
    private LocalDateTime fnextRetryTime;
    private LocalDateTime fprocessingStartedTime;
    private String fmessageNo;
    private String fcallbackStatus;
    private String fcallbackEventId;
    private String flastError;
    private LocalDateTime fcreateTime;
    private LocalDateTime fupdateTime;
}
