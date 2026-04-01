package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRowVO {
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String rowNo;
    private Integer level;
    private String lineType;
    private BigDecimal amount;
    private BigDecimal beginAmount;
    private BigDecimal currentAmount;
    private BigDecimal ytdAmount;
    private boolean drillable;
    private List<String> warnings;
}

