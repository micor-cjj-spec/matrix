package single.cjj.fi.ar.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_arap_doc")
public class BizfiFiArapDoc implements Serializable {
    @TableId("fid")
    private Long fid;
    private String fdoctype;
    private String fnumber;
    private LocalDate fdate;
    private String fcounterparty;
    private BigDecimal famount;
    private String fstatus;
    private String fremark;

    /** 差异化字段：付款申请 */
    private String fpayMethod;
    private LocalDate fplannedPayDate;

    /** 差异化字段：结算处理 */
    private String fsettleMethod;
    private String fwriteoffDetail;

    /** 差异化字段：暂估单 */
    private String fsourceBillNo;

    /** 联动凭证 */
    private Long fvoucherId;
    private String fvoucherNumber;

    private String fauditedBy;
    private LocalDateTime fauditedTime;
}
