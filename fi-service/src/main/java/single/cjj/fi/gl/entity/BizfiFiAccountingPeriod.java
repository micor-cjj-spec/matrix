package single.cjj.fi.gl.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会计期间实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_accounting_period")
public class BizfiFiAccountingPeriod implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    /** 组织ID */
    private Long forg;

    /** 会计期间 yyyy-MM */
    private String fperiod;

    /** 年度 */
    private Integer fyear;

    /** 月度 */
    private Integer fmonth;

    /** 开始日期 */
    private LocalDate fstartDate;

    /** 结束日期 */
    private LocalDate fendDate;

    /** 状态 OPEN/CLOSED */
    private String fstatus;

    /** 关账人 */
    private String fcloseBy;

    /** 关账时间 */
    private LocalDateTime fcloseTime;

    /** 备注 */
    private String fremark;

    /** 创建时间 */
    private LocalDateTime fcreatetime;

    /** 更新时间 */
    private LocalDateTime fupdatetime;
}
