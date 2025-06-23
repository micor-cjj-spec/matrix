package single.cjj.bizfi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.entity.BizfiBizUnit;
import single.cjj.bizfi.mapper.BizfiBizUnitMapper;
import single.cjj.bizfi.service.BizfiBizUnitService;

import java.util.Map;

/**
 * 业务单元服务实现
 */
@Service
public class BizfiBizUnitServiceImpl extends ServiceImpl<BizfiBizUnitMapper, BizfiBizUnit> implements BizfiBizUnitService {
    @Autowired
    private BizfiBizUnitMapper mapper;

    @Override
    public BizfiBizUnit add(BizfiBizUnit unit) {
        mapper.insert(unit);
        return unit;
    }

    @Override
    public BizfiBizUnit update(BizfiBizUnit unit) {
        mapper.updateById(unit);
        return mapper.selectById(unit.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiBizUnit get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiBizUnit> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiBizUnit> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText((String) query.get("fname"))) {
                wrapper.like(BizfiBizUnit::getFname, query.get("fname"));
            }
            if (StringUtils.hasText((String) query.get("fcode"))) {
                wrapper.like(BizfiBizUnit::getFcode, query.get("fcode"));
            }
            if (StringUtils.hasText((String) query.get("fstatus"))) {
                wrapper.eq(BizfiBizUnit::getFstatus, query.get("fstatus"));
            }
        }
        Page<BizfiBizUnit> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }
}
