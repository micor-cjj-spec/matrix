package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialIndicatorResultVO {
    private Long orgId;
    private String period;
    private String currency;
    private List<FinancialIndicatorRowVO> rows;
    private List<ReportCheckResultVO> checks;
    private List<String> warnings;
}
