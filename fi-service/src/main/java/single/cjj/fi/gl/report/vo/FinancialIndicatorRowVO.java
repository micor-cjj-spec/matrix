package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialIndicatorRowVO {
    private String code;
    private String name;
    private String category;
    private String formula;
    private BigDecimal value;
    private String unit;
    private String interpretation;
}
