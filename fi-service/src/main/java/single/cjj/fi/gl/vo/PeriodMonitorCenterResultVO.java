package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodMonitorCenterResultVO {
    private Long forg;
    private String period;
    private String periodSource;
    private String baseCurrency;
    private String currentPeriod;
    private String periodStatus;
    private Boolean foundationHealthy;
    private Integer totalModules;
    private Integer readyModules;
    private Integer warningModules;
    private Integer pendingModules;
    private Integer periodVoucherCount;
    private Integer pendingVoucherCount;
    private List<PeriodMonitorModuleVO> modules = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
