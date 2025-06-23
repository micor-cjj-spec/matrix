package single.cjj.bizfi.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 部门维度实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_dept_dim")
public class BizfiDeptDim implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId("fid")
    private Long fid;

    /** 名称 */
    private String fname;

    /** 编码 */
    private String fcode;

    /** 上级ID */
    private Long fparentid;

    /** 所属业务单元ID */
    private Long fbizunitid;

    /** 状态 */
    private String fstatus;
}
