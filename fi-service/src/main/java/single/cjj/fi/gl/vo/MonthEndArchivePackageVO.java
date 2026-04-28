package single.cjj.fi.gl.vo;

import lombok.Data;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCheckBatch;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCloseExecution;
import single.cjj.fi.gl.entity.BizfiFiPeriodRollover;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MonthEndArchivePackageVO {
    private Long forg;
    private String period;
    private String archiveStatus;
    private String conclusion;
    private LocalDateTime generatedAt;
    private Integer readinessScore;
    private Integer blockingCount;
    private Integer warningCount;
    private Integer pendingCount;
    private Integer passedCount;
    private Integer totalCheckCount;
    private Integer periodVoucherCount;
    private Integer postedVoucherCount;
    private Integer pendingVoucherCount;
    private Integer exceptionVoucherCount;
    private Boolean closeExecuted;
    private Boolean periodRolled;
    private Boolean hasWarnings;
    private BizfiFiMonthEndCheckBatch checkBatch;
    private BizfiFiMonthEndCloseExecution closeExecution;
    private BizfiFiPeriodRollover periodRollover;
    private MonthEndWorkbenchResultVO workbench;
    private List<MonthEndArchiveMilestoneVO> milestones = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
