package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnterpriseTaxRowVO {
    private String taxCode;
    private String taxName;
    private BigDecimal taxBase;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private String note;
}
