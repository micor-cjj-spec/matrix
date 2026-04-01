package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSheetDrillRowVO {
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String accountType;
    private String direction;
    private BigDecimal beginAmount;
    private BigDecimal endAmount;
    private String mappingSource;
}
