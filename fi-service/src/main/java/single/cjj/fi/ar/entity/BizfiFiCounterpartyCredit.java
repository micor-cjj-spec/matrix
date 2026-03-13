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
@TableName("bizfi_fi_counterparty_credit")
public class BizfiFiCounterpartyCredit implements Serializable {
    @TableId("fid")
    private Long fid;
    private String fcounterparty;
    /** AR / AP */
    private String fdocTypeRoot;
    private BigDecimal fcreditLimit;
    private Integer foverdueDaysThreshold;
    private Integer fenabled;
    private String fremark;
    private String fupdatedBy;
    private LocalDateTime fupdatedTime;
}
