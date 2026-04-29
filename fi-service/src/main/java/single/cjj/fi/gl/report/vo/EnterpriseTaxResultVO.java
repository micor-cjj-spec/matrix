package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnterpriseTaxResultVO {
    private Long orgId;
    private String period;
    private String currency;
    private BigDecimal revenueAmount;
    private BigDecimal netProfitAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal taxBurdenRate;
    private List<EnterpriseTaxRowVO> rows;
    private List<ReportCheckResultVO> checks;
    private List<String> warnings;
    private List<ReportMappingGapVO> mappingGaps;
}
