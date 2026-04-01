package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerDailyRowVO {
    private LocalDate bizDate;
    private Integer voucherCount;
    private BigDecimal openingBalance;
    private String openingDirection;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal closingBalance;
    private String closingDirection;
}
