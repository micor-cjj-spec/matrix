package single.cjj.bizfi.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 业务单元实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_biz_unit")
public class BizfiBizUnit implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId("fid")
    private Long fid;

    /** 名称 */
    private String fname;

    /** 编码 */
    private String fcode;

    /** 负责人ID */
    private Long fmanagerid;

    /** 状态 */
    private String fstatus;
}
