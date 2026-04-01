package single.cjj.fi.gl.report.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_report_item")
public class BizfiFiReportItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    private Long ftemplateId;
    private Long fparentId;
    private String fcode;
    private String fname;
    private String frowNo;
    private Integer flevel;
    private String flineType;
    private String fperiodMode;
    private String fsignRule;
    private Integer fdrillable;
    private Integer feditableAdjustment;
    private Integer fsort;
    private String fremark;
    private LocalDateTime fcreatetime;
    private LocalDateTime fupdatetime;
}

