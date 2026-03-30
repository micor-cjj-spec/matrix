package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerBalanceRowVO {
    private String accountCode;
    private String accountName;
    private String normalDirection;
    private BigDecimal openingBalance;
    private String openingDirection;
    private BigDecimal periodDebit;
    private BigDecimal periodCredit;
    private BigDecimal closingBalance;
    private String closingDirection;
    private Integer entryCount;
}
