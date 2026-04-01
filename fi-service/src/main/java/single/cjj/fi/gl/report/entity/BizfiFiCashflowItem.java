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
@TableName("bizfi_fi_cashflow_item")
public class BizfiFiCashflowItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    private String fcode;
    private String fname;
    private Long fparentId;
    private String fcategory;
    private String fdirection;
    private Integer fsort;
    private String fstatus;
    private String fremark;
    private LocalDateTime fcreatetime;
    private LocalDateTime fupdatetime;
}
