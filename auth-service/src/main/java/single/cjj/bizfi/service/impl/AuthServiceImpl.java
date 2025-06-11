package single.cjj.bizfi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import single.cjj.bizfi.entity.BizfiBaseUser;
import single.cjj.bizfi.mapper.AuthMapper;
import single.cjj.bizfi.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthMapper authMapper;

    @Override
    public BizfiBaseUser getUserByAccount(String account) {
        return authMapper.selectOne(
                new LambdaQueryWrapper<BizfiBaseUser>()
                        .eq(BizfiBaseUser::getFnumber, account)
                        .or()
                        .eq(BizfiBaseUser::getFphone, account)
                        .or()
                        .eq(BizfiBaseUser::getFemail, account)
        );
    }
}
