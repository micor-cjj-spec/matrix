package single.cjj.fi.gl.init.entity;

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
@TableName("bizfi_fi_subject_opening_balance")
public class BizfiFiSubjectOpeningBalance implements Serializable {
    @TableId("fid")
    private Long fid;
    private Long forg;
    private String fperiod;
    private Long faccountId;
    private String faccountCode;
    private String faccountName;
    private BigDecimal fdebitAmount;
    private BigDecimal fcreditAmount;
    private String fremark;
    private LocalDateTime fcreatetime;
    private LocalDateTime fupdatetime;
}
