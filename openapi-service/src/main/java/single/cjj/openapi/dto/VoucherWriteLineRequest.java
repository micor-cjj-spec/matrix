package single.cjj.openapi.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherWriteLineRequest {

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
