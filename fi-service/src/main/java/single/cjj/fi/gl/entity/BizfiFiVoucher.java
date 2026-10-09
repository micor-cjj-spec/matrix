package single.cjj.fi.gl.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
 * 财务凭证实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_voucher")
public class BizfiFiVoucher implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId("fid")
    private Long fid;

    /** 租户 */
    @TableField("tenant_id")
    private String tenantId;

    /** 业务组织 */
    @TableField("org_id")
    private String organizationId;

    /** 账簿 */
    @TableField("book_id")
    private String bookId;

    /** OpenAPI写入来源请求，用于消费幂等 */
    @TableField("source_request_id")
    private String sourceRequestId;

    /** 凭证编号 */
    private String fnumber;

    /** 日期 */
    private LocalDate fdate;

    /** 摘要 */
    private String fsummary;

    /** 金额 */
    private BigDecimal famount;

    /** 状态：DRAFT/SUBMITTED/AUDITED/POSTED */
    private String fstatus;

    /** 制单人 */
    private String fcreatedBy;

    /** 制单时间 */
    private LocalDateTime fcreatedTime;

    /** 审核人 */
    private String fauditedBy;

    /** 审核时间 */
    private LocalDateTime fauditedTime;

    /** 过账人 */
    private String fpostedBy;

    /** 过账时间 */
    private LocalDateTime fpostedTime;

    /** 备注 */
    private String fremark;
}
