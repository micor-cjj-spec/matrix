package single.cjj.bizfi.service;

import single.cjj.bizfi.dto.EmailSendRequest;
import single.cjj.bizfi.dto.LoginRequest;
import single.cjj.bizfi.dto.LoginResponse;
import single.cjj.bizfi.dto.RegisterRequest;

public interface BizfiAuthLoginService {
    LoginResponse loginByAccount(LoginRequest request);

    LoginResponse loginByPhone(LoginRequest request);

    Boolean sendEmailCode(EmailSendRequest request);     // 返回是否发送成功

    Boolean register(RegisterRequest request);           // 注册成功返回true，失败抛异常

    Boolean check(String type, String value);            // 唯一返回true，不唯一返回false
}
