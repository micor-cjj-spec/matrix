package single.cjj.fi.gl.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 凭证明细行
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_voucher_line")
public class BizfiFiVoucherLine implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    /** 凭证ID */
    private Long fvoucherId;

    /** 行号 */
    private Integer flineNo;

    /** 科目编码 */
    private String faccountCode;

    /** 行摘要 */
    private String fsummary;

    /** 借方金额 */
    private BigDecimal fdebitAmount;

    /** 贷方金额 */
    private BigDecimal fcreditAmount;

    /** 币种 */
    private String fcurrency;

    /** 汇率 */
    private BigDecimal frate;

    /** 原币金额 */
    private BigDecimal foriginalAmount;
}
