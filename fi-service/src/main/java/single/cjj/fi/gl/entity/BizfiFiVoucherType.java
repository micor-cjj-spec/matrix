package single.cjj.fi.gl.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 凭证字实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_voucher_type")
public class BizfiFiVoucherType implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    /** 组织ID */
    private Long forg;

    /** 凭证字编码 */
    private String fcode;

    /** 凭证字名称 */
    private String fname;

    /** 编号前缀 */
    private String fprefix;

    /** 排序 */
    private Integer fsort;

    /** 状态 ENABLED/DISABLED */
    private String fstatus;

    /** 备注 */
    private String fremark;

    /** 创建时间 */
    private LocalDateTime fcreatetime;

    /** 更新时间 */
    private LocalDateTime fupdatetime;
}
