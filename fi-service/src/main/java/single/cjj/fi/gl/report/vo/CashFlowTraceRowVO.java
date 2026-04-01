package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowTraceRowVO {
    private Long voucherId;
    private String voucherNumber;
    private String voucherDate;
    private String summary;
    private String cashAccountCodes;
    private String counterpartyAccountCodes;
    private String cashflowItemCode;
    private String cashflowItemName;
    private String categoryCode;
    private String categoryName;
    private String sourceType;
    private BigDecimal cashInAmount;
    private BigDecimal cashOutAmount;
    private BigDecimal netAmount;
    private Integer cashEntryCount;
    private Integer counterpartyEntryCount;
    private String postedBy;
    private String postedTime;
    private String reason;
}
