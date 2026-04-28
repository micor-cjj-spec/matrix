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
@TableName("bizfi_fi_month_end_check_batch")
public class BizfiFiMonthEndCheckBatch implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;
    private String fbatchNo;
    private Long forg;
    private String fperiod;
    private String fperiodSource;
    private String fbaseCurrency;
    private String fcurrentPeriod;
    private String fperiodStatus;
    private String fcloseStatus;
    private Integer freadinessScore;
    private Boolean fcanClose;
    private Integer fblockingCount;
    private Integer fwarningCount;
    private Integer fpendingCount;
    private Integer fpassedCount;
    private Integer ftotalCheckCount;
    private Integer fperiodVoucherCount;
    private Integer fpostedVoucherCount;
    private Integer fpendingVoucherCount;
    private Integer fexceptionVoucherCount;
    private String fapplicationStatus;
    private String fsnapshotJson;
    private String fremark;
    private String fcreatedBy;
    private LocalDateTime fcreatedTime;
    private String fsubmittedBy;
    private LocalDateTime fsubmittedTime;
    private String fapprovedBy;
    private LocalDateTime fapprovedTime;
    private LocalDateTime fupdateTime;
}

