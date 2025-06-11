package single.cjj.bizfi.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 基础用户信息表
 * </p>
 *
 * @author micor
 * @since 2025-06-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_base_user")
public class BizfiBaseUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId("fid")
    private Long fid;

    /**
     * 团队ID
     */
    private Long ftid;

    /**
     * 工号
     */
    private String fnumber;

    /**
     * 手机号码
     */
    private String fphone;

    /**
     * 电子邮箱
     */
    private String femail;

    /**
     * 数据状态（启用/停用/草稿等）
     */
    private String fstatus;

    /**
     * 性别（男/女/其他）
     */
    private String fgender;

    /**
     * 出生日期
     */
    private LocalDateTime fbirthday;

    /**
     * 身份证号码
     */
    private String fidcard;

    /**
     * 用户头像（头像URL）
     */
    private String favatar;

    /**
     * 昵称
     */
    private String fnickname;

    /**
     * 姓名全拼
     */
    private String ffullpinyin;

    /**
     * 姓名拼音缩写
     */
    private String fsimplepinyin;

    /**
     * 所属部门ID
     */
    private Long fdptid;

    /**
     * 岗位ID
     */
    private Long fpositionid;

    /**
     * 排序编号
     */
    private String fsortcode;

    /**
     * 单据状态字段
     */
    private String fbillssatusfield;

    /**
     * 入职日期
     */
    private LocalDateTime fhiredate;

    /**
     * 是否启用（Y/N）
     */
    private String fenable;

    /**
     * 创建人ID
     */
    private Long fcreatorid;

    /**
     * 创建时间
     */
    private LocalDateTime fcreatetime;

    /**
     * 最后修改人ID
     */
    private Long fmodifierid;

    /**
     * 最后修改时间
     */
    private LocalDateTime fmodifytime;

    /**
     * 停用人ID
     */
    private Long fdisablerid;

    /**
     * 停用时间
     */
    private LocalDateTime fdisabledate;

    /**
     * 数据来源（导入/注册/同步）
     */
    private String fsource;

    /**
     * 维护方式（手动/接口）
     */
    private String fmaintain;

    /**
     * 有效开始时间
     */
    private LocalDateTime fstartdate;

    /**
     * 有效结束时间
     */
    private LocalDateTime fenddate;

    /**
     * 主用户ID（用于主从账号绑定）
     */
    private Long fmasterid;

    /**
     * 个性头像或自定义形象
     */
    private String fheadsculpture;

    /**
     * 真实姓名
     */
    private String ftruename;

    /**
     * 所属国家ID
     */
    private Long fcountryid;
}
