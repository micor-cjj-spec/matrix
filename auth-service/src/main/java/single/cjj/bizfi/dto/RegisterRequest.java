// RegisterRequest.java
package single.cjj.bizfi.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String code; // 邮箱验证码
}
