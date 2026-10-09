package single.cjj.fi.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceDashboardOverviewVO {

    private Long forg;
    private String period;
    private Integer monthCloseProgress;
    private Integer monthClosePassedCount;
    private Integer monthCloseTotalCount;
    private Integer monthCloseBlockingCount;
    private Boolean canClose;
    private Integer monthVoucherCount;
    private Integer postedVoucherCount;
    private Integer pendingVoucherCount;
    private Integer exceptionVoucherCount;
    private Integer receivableWarningCount;
    private Integer payableWarningCount;
    private LocalDateTime generatedAt;
    private List<FocusItem> focusItems = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FocusItem {
        private String code;
        private String name;
        private String status;
        private Integer count;
        private String path;
    }
}
