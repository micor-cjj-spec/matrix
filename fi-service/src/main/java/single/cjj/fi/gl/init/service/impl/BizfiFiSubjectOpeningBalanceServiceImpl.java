package single.cjj.fi.gl.init.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.init.entity.BizfiFiSubjectOpeningBalance;
import single.cjj.fi.gl.init.mapper.BizfiFiSubjectOpeningBalanceMapper;
import single.cjj.fi.gl.init.service.BizfiFiSubjectOpeningBalanceService;

import java.util.Map;

@Service
public class BizfiFiSubjectOpeningBalanceServiceImpl extends ServiceImpl<BizfiFiSubjectOpeningBalanceMapper, BizfiFiSubjectOpeningBalance>
        implements BizfiFiSubjectOpeningBalanceService {

    @Override
    public BizfiFiSubjectOpeningBalance add(BizfiFiSubjectOpeningBalance item) {
        baseMapper.insert(item);
        return item;
    }

    @Override
    public BizfiFiSubjectOpeningBalance update(BizfiFiSubjectOpeningBalance item) {
        baseMapper.updateById(item);
        return baseMapper.selectById(item.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return baseMapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiSubjectOpeningBalance get(Long fid) {
        return baseMapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiSubjectOpeningBalance> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiSubjectOpeningBalance> wrapper = new LambdaQueryWrapper<>();
        if (query.get("forg") != null) {
            wrapper.eq(BizfiFiSubjectOpeningBalance::getForg, query.get("forg"));
        }
        if (StringUtils.hasText((String) query.get("fperiod"))) {
            wrapper.eq(BizfiFiSubjectOpeningBalance::getFperiod, query.get("fperiod"));
        }
        if (StringUtils.hasText((String) query.get("faccountCode"))) {
            wrapper.like(BizfiFiSubjectOpeningBalance::getFaccountCode, query.get("faccountCode"));
        }
        if (StringUtils.hasText((String) query.get("faccountName"))) {
            wrapper.like(BizfiFiSubjectOpeningBalance::getFaccountName, query.get("faccountName"));
        }
        wrapper.orderByDesc(BizfiFiSubjectOpeningBalance::getFperiod)
                .orderByAsc(BizfiFiSubjectOpeningBalance::getFaccountCode)
                .orderByAsc(BizfiFiSubjectOpeningBalance::getFid);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
