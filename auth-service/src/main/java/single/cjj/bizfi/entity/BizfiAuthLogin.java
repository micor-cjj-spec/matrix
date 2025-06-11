package single.cjj.bizfi.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 用户登录认证表
 * </p>
 *
 * @author micor
 * @since 2025-06-06
 */
@Getter
@Setter
@TableName("bizfi_auth_login")
public class BizfiAuthLogin implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId("fid")
    private Long fid;

    /**
     * 关联用户FID
     */
    private Long fuserid;

    /**
     * 登录密码（MD5加密）
     */
    private String fpassword;

    /**
     * 创建时间
     */
    private LocalDateTime fcreatetime;

    /**
     * 修改时间
     */
    private LocalDateTime fmodifytime;

    /**
     * 创建人ID
     */
    private Long fcreatorid;

    /**
     * 修改人ID
     */
    private Long fmodifierid;
}
