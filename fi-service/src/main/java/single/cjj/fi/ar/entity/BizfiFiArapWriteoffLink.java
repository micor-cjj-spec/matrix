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
@TableName("bizfi_fi_arap_writeoff_link")
public class BizfiFiArapWriteoffLink implements Serializable {
    @TableId("fid")
    private Long fid;
    private Long flogId;
    private String fplanCode;
    private String fdocTypeRoot;
    private String fcounterparty;
    private Long fsourceDocId;
    private String fsourceDocNumber;
    private String fsourceDocType;
    private Long ftargetDocId;
    private String ftargetDocNumber;
    private String ftargetDocType;
    private BigDecimal famount;
    private Integer fautoFlag;
    private String foperator;
    private LocalDateTime foperateTime;
    private String fremark;
}
