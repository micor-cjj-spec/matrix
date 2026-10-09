package single.cjj.openapi.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenVoucherLineResponse {

    private String lineId;
    private Integer lineNumber;
    private String accountCode;
    private String summary;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String currencyCode;
    private BigDecimal exchangeRate;
    private BigDecimal originalAmount;
    private String cashflowItemCode;
}
