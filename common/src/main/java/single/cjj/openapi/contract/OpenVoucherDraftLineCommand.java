package single.cjj.openapi.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenVoucherDraftLineCommand {

    private Integer lineNo;
    private String accountCode;
    private String summary;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String currency;
    private BigDecimal rate;
    private BigDecimal originalAmount;
    private String cashflowItem;
}
