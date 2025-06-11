package single.cjj.bizfi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import single.cjj.bizfi.entity.BizfiBaseUser;
import single.cjj.bizfi.mapper.BizfiBaseUserMapper;
import single.cjj.bizfi.service.BizfiBaseUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 基础用户信息表 服务实现类
 * </p>
 *
 * @author micor
 * @since 2025-06-04
 */
@Service
public class BizfiBaseUserServiceImpl extends ServiceImpl<BizfiBaseUserMapper, BizfiBaseUser> implements BizfiBaseUserService {

    @Autowired
    private BizfiBaseUserMapper bizfiBaseUserMapper;

    @Override
    public BizfiBaseUser getUserByAccount(String account) {
        return bizfiBaseUserMapper.selectOne(
                new LambdaQueryWrapper<BizfiBaseUser>()
                        .eq(BizfiBaseUser::getFnumber, account)
                        .or()
                        .eq(BizfiBaseUser::getFphone, account)
                        .or()
                        .eq(BizfiBaseUser::getFemail, account)
        );
    }
}
