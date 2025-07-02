package single.cjj.fi.gl.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 财务科目实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_account")
public class BizfiFiAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId("fid")
    private Long fid;

    /** 科目编码 */
    private String fcode;

    /** 科目名称 */
    private String fname;

    /** 组织ID */
    private Long forg;

    /** 全称 */
    private String flongName;

    /** 科目类型 */
    private String ftype;

    /** 上级科目ID */
    private Long fparent;

    /** 利润表类型 */
    private String fpltype;

    /** 方向 */
    private String fdirection;

    /** 是否明细科目 */
    private Integer fisDetail;

    /** 关联报表项目ID */
    private Long freportItem;

    /** 一级科目ID */
    private Long flevel1;

    /** 分录控制 */
    private String fentryControl;

    /** 控制级别 */
    private String fcontrolLevel;

    /** 是否允许子科目 */
    private Integer fallowChild;

    /** 是否手工录入 */
    private Integer fmanualEntry;

    /** 是否现金科目 */
    private Integer fcash;

    /** 是否银行科目 */
    private Integer fbank;

    /** 是否等价物科目 */
    private Integer fequivalent;

    /** 是否可录凭证分录 */
    private Integer fisEntry;

    /** 是否通知 */
    private Integer fnotice;

    /** 是否外币科目 */
    private Integer fexchange;

    /** 是否数量核算 */
    private Integer fqtyAccounting;

    /** 创建时间 */
    private LocalDateTime fcreatetime;

    /** 更新时间 */
    private LocalDateTime fupdatetime;
}
