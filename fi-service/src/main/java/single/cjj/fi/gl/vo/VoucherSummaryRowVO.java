package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherSummaryRowVO {
    private LocalDate bizDate;
    private Integer voucherCount;
    private BigDecimal totalAmount;
    private Integer draftCount;
    private Integer submittedCount;
    private Integer auditedCount;
    private Integer postedCount;
    private Integer rejectedCount;
    private Integer reversedCount;
    private BigDecimal postedAmount;
}
