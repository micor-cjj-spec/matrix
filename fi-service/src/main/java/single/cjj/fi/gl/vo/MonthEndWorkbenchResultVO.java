package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthEndWorkbenchResultVO {
    private Long forg;
    private String period;
    private String periodSource;
    private String baseCurrency;
    private String currentPeriod;
    private String periodStatus;
    private String closeStatus;
    private Integer readinessScore;
    private Boolean canClose;
    private Integer totalCheckCount;
    private Integer passedCount;
    private Integer warningCount;
    private Integer blockingCount;
    private Integer pendingCount;
    private Integer periodVoucherCount;
    private Integer postedVoucherCount;
    private Integer pendingVoucherCount;
    private Integer exceptionVoucherCount;
    private LocalDateTime checkedAt;
    private List<MonthEndCheckItemVO> checkItems = new ArrayList<>();
    private List<MonthEndStepVO> steps = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}

