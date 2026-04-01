package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerBookRowVO {
    private String dimensionCode;
    private String dimensionName;
    private String accountCode;
    private String accountName;
    private LocalDate voucherDate;
    private String voucherNumber;
    private String summary;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal balance;
    private String balanceDirection;
}
