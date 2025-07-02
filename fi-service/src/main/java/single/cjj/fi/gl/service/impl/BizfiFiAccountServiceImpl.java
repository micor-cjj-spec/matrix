package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiAccount;
import single.cjj.fi.gl.mapper.BizfiFiAccountMapper;
import single.cjj.fi.gl.service.BizfiFiAccountService;

import java.util.Map;

/**
 * 会计科目服务实现
 */
@Service
public class BizfiFiAccountServiceImpl extends ServiceImpl<BizfiFiAccountMapper, BizfiFiAccount>
        implements BizfiFiAccountService {

    @Autowired
    private BizfiFiAccountMapper mapper;

    @Override
    public BizfiFiAccount add(BizfiFiAccount account) {
        mapper.insert(account);
        return account;
    }

    @Override
    public BizfiFiAccount update(BizfiFiAccount account) {
        mapper.updateById(account);
        return mapper.selectById(account.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiAccount get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiAccount> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiAccount> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText((String) query.get("fcode"))) {
                wrapper.like(BizfiFiAccount::getFcode, query.get("fcode"));
            }
            if (StringUtils.hasText((String) query.get("fname"))) {
                wrapper.like(BizfiFiAccount::getFname, query.get("fname"));
            }
        }
        Page<BizfiFiAccount> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }
}
