package single.cjj.fi.gl.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_period_rollover")
public class BizfiFiPeriodRollover implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;
    private String frolloverNo;
    private Long fcloseExecutionId;
    private String fcloseExecutionNo;
    private Long forg;
    private String ffromPeriod;
    private String ftoPeriod;
    private Long fnextPeriodId;
    private Long fconfigId;
    private String fbeforeCurrentPeriod;
    private String fafterCurrentPeriod;
    private Boolean fcreatedNextPeriod;
    private String frolloverStatus;
    private String foperator;
    private String fremark;
    private LocalDateTime frolledTime;
    private LocalDateTime fcreatedTime;
}
