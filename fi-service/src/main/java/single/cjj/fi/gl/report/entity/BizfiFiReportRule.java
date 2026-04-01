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
@TableName("bizfi_fi_report_rule")
public class BizfiFiReportRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    private Long fitemId;
    private String fruleType;
    private String fruleExpr;
    private String fsignTransform;
    private Integer fpriority;
    private String fstatus;
    private LocalDateTime fcreatetime;
    private LocalDateTime fupdatetime;
}

