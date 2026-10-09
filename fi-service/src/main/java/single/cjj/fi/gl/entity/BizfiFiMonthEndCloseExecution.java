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
@TableName("bizfi_fi_month_end_close_execution")
public class BizfiFiMonthEndCloseExecution implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;
    private String fexecutionNo;
    private Long fbatchId;
    private String fbatchNo;
    private Long forg;
    private String fperiod;
    private Long fperiodId;
    private String fbeforeStatus;
    private String fafterStatus;
    private String fexecutionStatus;
    private String fcheckSnapshotJson;
    private String foperator;
    private String fremark;
    private LocalDateTime fexecutedTime;
    private LocalDateTime fcreatedTime;
}
