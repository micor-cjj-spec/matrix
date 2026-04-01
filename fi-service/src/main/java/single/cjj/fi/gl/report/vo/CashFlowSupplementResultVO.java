package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowSupplementResultVO {
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
    private Integer cashAccountCount;
    private Integer cashflowItemCount;
    private BigDecimal cashInAmount;
    private BigDecimal cashOutAmount;
    private BigDecimal netAmount;
    private List<CashFlowSupplementTaskVO> tasks;
    private List<CashFlowSupplementCategoryVO> categories;
    private List<CashFlowSupplementVoucherVO> pendingVouchers;
    private List<String> warnings;
}
