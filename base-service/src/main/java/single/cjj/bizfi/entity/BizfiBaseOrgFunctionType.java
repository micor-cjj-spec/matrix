package single.cjj.bizfi.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织职能类型实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_base_org_function_type")
public class BizfiBaseOrgFunctionType implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId("fid")
    private Long fid;

    /** 名称 */
    private String fname;

    /** 编码 */
    private String fnumber;

    /** 类型，BU/OT */
    private String ftype;

    /** 基本职能，1表示有，0表示无 */
    private Integer fbasefunction;

    /** 自定义，1表示有，0表示无 */
    private Integer fcustom;

    /** 创建时间 */
    private LocalDateTime fcreatedate;

    /** 修改时间 */
    private LocalDateTime fmodifydate;

    /** 创建人 */
    private String fcreator;

    /** 修改人 */
    private String fmodifier;
}
