package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerDimensionBalanceRowVO {
    private String dimensionCode;
    private String dimensionName;
    private String accountCode;
    private String accountName;
    private BigDecimal openingBalance;
    private String openingDirection;
    private BigDecimal periodDebit;
    private BigDecimal periodCredit;
    private BigDecimal closingBalance;
    private String closingDirection;
    private Integer entryCount;
}
