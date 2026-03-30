package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowSupplementVoucherVO {
    private Long voucherId;
    private String voucherNumber;
    private String voucherDate;
    private String summary;
    private String sourceType;
    private String cashflowItemCode;
    private String cashflowItemName;
    private String categoryName;
    private BigDecimal netAmount;
    private String issue;
    private String suggestion;
}
