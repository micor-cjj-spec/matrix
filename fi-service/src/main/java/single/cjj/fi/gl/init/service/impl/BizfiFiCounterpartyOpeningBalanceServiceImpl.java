package single.cjj.fi.gl.init.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.init.entity.BizfiFiCounterpartyOpeningBalance;
import single.cjj.fi.gl.init.mapper.BizfiFiCounterpartyOpeningBalanceMapper;
import single.cjj.fi.gl.init.service.BizfiFiCounterpartyOpeningBalanceService;

import java.util.Map;

@Service
public class BizfiFiCounterpartyOpeningBalanceServiceImpl extends ServiceImpl<BizfiFiCounterpartyOpeningBalanceMapper, BizfiFiCounterpartyOpeningBalance>
        implements BizfiFiCounterpartyOpeningBalanceService {

    @Override
    public BizfiFiCounterpartyOpeningBalance add(BizfiFiCounterpartyOpeningBalance item) {
        baseMapper.insert(item);
        return item;
    }

    @Override
    public BizfiFiCounterpartyOpeningBalance update(BizfiFiCounterpartyOpeningBalance item) {
        baseMapper.updateById(item);
        return baseMapper.selectById(item.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return baseMapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiCounterpartyOpeningBalance get(Long fid) {
        return baseMapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiCounterpartyOpeningBalance> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiCounterpartyOpeningBalance> wrapper = new LambdaQueryWrapper<>();
        if (query.get("forg") != null) {
            wrapper.eq(BizfiFiCounterpartyOpeningBalance::getForg, query.get("forg"));
        }
        if (StringUtils.hasText((String) query.get("fperiod"))) {
            wrapper.eq(BizfiFiCounterpartyOpeningBalance::getFperiod, query.get("fperiod"));
        }
        if (StringUtils.hasText((String) query.get("fcounterpartyType"))) {
            wrapper.eq(BizfiFiCounterpartyOpeningBalance::getFcounterpartyType, query.get("fcounterpartyType"));
        }
        if (StringUtils.hasText((String) query.get("fcounterpartyName"))) {
            wrapper.like(BizfiFiCounterpartyOpeningBalance::getFcounterpartyName, query.get("fcounterpartyName"));
        }
        wrapper.orderByDesc(BizfiFiCounterpartyOpeningBalance::getFperiod)
                .orderByAsc(BizfiFiCounterpartyOpeningBalance::getFcounterpartyType)
                .orderByAsc(BizfiFiCounterpartyOpeningBalance::getFcounterpartyName)
                .orderByAsc(BizfiFiCounterpartyOpeningBalance::getFid);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
