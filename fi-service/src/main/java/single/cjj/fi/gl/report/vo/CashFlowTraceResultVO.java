package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowTraceResultVO {
    private Long orgId;
    private String period;
    private String startDate;
    private String endDate;
    private String currency;
    private Long postedVoucherCount;
    private Long cashVoucherCount;
    private Long directCount;
    private Long heuristicCount;
    private Long unknownCount;
    private Long mixedCount;
    private Long transferCount;
    private BigDecimal cashInAmount;
    private BigDecimal cashOutAmount;
    private BigDecimal netAmount;
    private List<CashFlowTraceRowVO> records;
    private List<String> warnings;
}
