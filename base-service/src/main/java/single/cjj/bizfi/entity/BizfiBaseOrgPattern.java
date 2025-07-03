package single.cjj.bizfi.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织形态实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_base_org_pattern")
public class BizfiBaseOrgPattern implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId("fid")
    private Long fid;

    /** 单据编号 */
    private String fbillno;

    /** 名称 */
    private String fname;

    /** 组织形态 */
    private String fpattern;

    /** 是否可用，1可用，0不可用 */
    private Integer fenable;

    /** 创建时间 */
    private LocalDateTime fcreatedate;

    /** 创建人 */
    private String fcreator;

    /** 修改时间 */
    private LocalDateTime fmodifydate;

    /** 修改人 */
    private String fmodifier;
}
