package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherSummaryResultVO {
    private String startDate;
    private String endDate;
    private String status;
    private String summaryKeyword;
    private Integer totalCount;
    private BigDecimal totalAmount;
    private Integer draftCount;
    private Integer submittedCount;
    private Integer auditedCount;
    private Integer postedCount;
    private Integer rejectedCount;
    private Integer reversedCount;
    private BigDecimal postedAmount;
    private List<VoucherSummaryRowVO> rows = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
