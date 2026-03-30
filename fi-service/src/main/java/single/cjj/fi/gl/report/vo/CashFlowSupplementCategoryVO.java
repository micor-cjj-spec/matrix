package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowSupplementCategoryVO {
    private String categoryCode;
    private String categoryName;
    private Integer voucherCount;
    private BigDecimal cashInAmount;
    private BigDecimal cashOutAmount;
    private BigDecimal netAmount;
    private Integer directCount;
    private Integer heuristicCount;
    private Integer pendingCount;
}
