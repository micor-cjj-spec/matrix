package single.cjj.fi.gl.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织财务配置实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_org_finance_config")
public class BizfiFiOrgFinanceConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    /** 组织ID */
    private Long forg;

    /** 本位币 */
    private String fbaseCurrency;

    /** 当前期间 yyyy-MM */
    private String fcurrentPeriod;

    /** 期间控制模式 STRICT/FLEXIBLE */
    private String fperiodControlMode;

    /** 默认凭证字编码 */
    private String fdefaultVoucherType;

    /** 状态 ENABLED/DISABLED */
    private String fstatus;

    /** 备注 */
    private String fremark;

    /** 创建时间 */
    private LocalDateTime fcreatetime;

    /** 更新时间 */
    private LocalDateTime fupdatetime;
}
