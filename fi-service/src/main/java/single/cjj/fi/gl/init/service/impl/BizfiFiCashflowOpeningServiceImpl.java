package single.cjj.fi.gl.init.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.init.entity.BizfiFiCashflowOpening;
import single.cjj.fi.gl.init.mapper.BizfiFiCashflowOpeningMapper;
import single.cjj.fi.gl.init.service.BizfiFiCashflowOpeningService;

import java.util.Map;

@Service
public class BizfiFiCashflowOpeningServiceImpl extends ServiceImpl<BizfiFiCashflowOpeningMapper, BizfiFiCashflowOpening>
        implements BizfiFiCashflowOpeningService {

    @Override
    public BizfiFiCashflowOpening add(BizfiFiCashflowOpening item) {
        baseMapper.insert(item);
        return item;
    }

    @Override
    public BizfiFiCashflowOpening update(BizfiFiCashflowOpening item) {
        baseMapper.updateById(item);
        return baseMapper.selectById(item.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return baseMapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiCashflowOpening get(Long fid) {
        return baseMapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiCashflowOpening> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiCashflowOpening> wrapper = new LambdaQueryWrapper<>();
        if (query.get("forg") != null) {
            wrapper.eq(BizfiFiCashflowOpening::getForg, query.get("forg"));
        }
        if (StringUtils.hasText((String) query.get("fperiod"))) {
            wrapper.eq(BizfiFiCashflowOpening::getFperiod, query.get("fperiod"));
        }
        if (StringUtils.hasText((String) query.get("fcashflowCode"))) {
            wrapper.like(BizfiFiCashflowOpening::getFcashflowCode, query.get("fcashflowCode"));
        }
        if (StringUtils.hasText((String) query.get("fcashflowName"))) {
            wrapper.like(BizfiFiCashflowOpening::getFcashflowName, query.get("fcashflowName"));
        }
        wrapper.orderByDesc(BizfiFiCashflowOpening::getFperiod)
                .orderByAsc(BizfiFiCashflowOpening::getFcashflowCode)
                .orderByAsc(BizfiFiCashflowOpening::getFid);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
