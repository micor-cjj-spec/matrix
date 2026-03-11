package single.cjj.fi.gl.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 总账分录（过账结果）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_gl_entry")
public class BizfiFiGlEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    private Long fvoucherId;
    private Long fvoucherLineId;
    private String fvoucherNumber;
    private LocalDate fvoucherDate;
    private String faccountCode;
    private String fsummary;
    private BigDecimal fdebitAmount;
    private BigDecimal fcreditAmount;
    private LocalDateTime fpostedTime;
    private String fpostedBy;
}
