package single.cjj.bizfi.service;

import single.cjj.bizfi.dto.LoginRequest;
import single.cjj.bizfi.dto.LoginResponse;

public interface BizfiAuthLoginService {
    LoginResponse loginByAccount(LoginRequest request);

    LoginResponse loginByPhone(LoginRequest request);
}
