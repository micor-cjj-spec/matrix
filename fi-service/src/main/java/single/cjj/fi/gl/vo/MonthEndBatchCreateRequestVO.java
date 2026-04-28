package single.cjj.fi.gl.vo;

import lombok.Data;

@Data
public class MonthEndBatchCreateRequestVO {
    private Long forg;
    private String period;
    private String createdBy;
    private String remark;
}

