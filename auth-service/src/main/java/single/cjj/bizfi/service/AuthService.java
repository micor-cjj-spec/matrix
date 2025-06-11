package single.cjj.bizfi.service;

import single.cjj.bizfi.entity.BizfiBaseUser;

public interface AuthService {
    BizfiBaseUser getUserByAccount(String account);
}