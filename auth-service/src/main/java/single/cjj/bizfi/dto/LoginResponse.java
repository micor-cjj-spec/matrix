package single.cjj.bizfi.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户FID
     */
    private Long fid;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 登录成功后的token（可选）
     */
    private String token;

    /**
     * token过期时间（秒）（可选）
     */
    private Long expireIn;
}
