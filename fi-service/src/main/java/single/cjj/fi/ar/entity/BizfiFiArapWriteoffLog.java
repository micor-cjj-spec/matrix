package single.cjj.fi.ar.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_arap_writeoff_log")
public class BizfiFiArapWriteoffLog implements Serializable {
    @TableId("fid")
    private Long fid;
    private String fplanCode;
    private String fdocTypeRoot;
    private String fcounterparty;
    private String fmode;
    private Integer fsourceDocCount;
    private Integer ftargetDocCount;
    private Integer flinkCount;
    private BigDecimal ftotalAmount;
    private String fstatus;
    private String fmessage;
    private String foperator;
    private LocalDateTime foperateTime;
}
