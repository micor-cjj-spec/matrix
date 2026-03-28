package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportQueryResultVO {
    private String reportType;
    private Long templateId;
    private String templateName;
    private Long orgId;
    private String period;
    private String startPeriod;
    private String endPeriod;
    private String currency;
    private List<ReportRowVO> rows;
    private List<ReportCheckResultVO> checks;
    private List<String> warnings;
}
