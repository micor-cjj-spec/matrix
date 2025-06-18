package single.cjj.bizfi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.entity.BizfiBaseUser;
import single.cjj.bizfi.mapper.BizfiBaseUserMapper;
import single.cjj.bizfi.service.BizfiBaseUserService;

import java.util.List;
import java.util.Map;

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

    @Override
    public BizfiBaseUser addUser(BizfiBaseUser user) {
        bizfiBaseUserMapper.insert(user);
        return user;
    }

    @Override
    public boolean deleteUser(Long fid) {
        return bizfiBaseUserMapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiBaseUser updateUser(BizfiBaseUser user) {
        bizfiBaseUserMapper.updateById(user);
        return bizfiBaseUserMapper.selectById(user.getFid());
    }
    @Override
    public IPage<BizfiBaseUser> getUserList(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiBaseUser> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText((String) query.get("ftruename"))) {
                wrapper.like(BizfiBaseUser::getFtruename, query.get("ftruename"));
            }
            if (StringUtils.hasText((String) query.get("femail"))) {
                wrapper.like(BizfiBaseUser::getFemail, query.get("femail"));
            }
            if (StringUtils.hasText((String) query.get("fstatus"))) {
                wrapper.eq(BizfiBaseUser::getFstatus, query.get("fstatus"));
            }
        }
        Page<BizfiBaseUser> pageObj = new Page<>(page, size);
        return bizfiBaseUserMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public BizfiBaseUser getUserById(Long fid) {
        return bizfiBaseUserMapper.selectById(fid);
    }

    @Override
    public boolean deleteBatch(List<Long> fids) {
        return bizfiBaseUserMapper.deleteBatchIds(fids) > 0;
    }
}
