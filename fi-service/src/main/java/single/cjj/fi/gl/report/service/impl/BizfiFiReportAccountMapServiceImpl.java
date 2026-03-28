package single.cjj.fi.gl.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import single.cjj.fi.gl.report.entity.BizfiFiReportAccountMap;
import single.cjj.fi.gl.report.mapper.BizfiFiReportAccountMapMapper;
import single.cjj.fi.gl.report.service.BizfiFiReportAccountMapService;

import java.util.Map;

@Service
public class BizfiFiReportAccountMapServiceImpl
        extends ServiceImpl<BizfiFiReportAccountMapMapper, BizfiFiReportAccountMap>
        implements BizfiFiReportAccountMapService {

    @Autowired
    private BizfiFiReportAccountMapMapper mapper;

    @Override
    public BizfiFiReportAccountMap add(BizfiFiReportAccountMap accountMap) {
        mapper.insert(accountMap);
        return accountMap;
    }

    @Override
    public BizfiFiReportAccountMap update(BizfiFiReportAccountMap accountMap) {
        mapper.updateById(accountMap);
        return mapper.selectById(accountMap.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiReportAccountMap get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiReportAccountMap> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiReportAccountMap> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.get("ftemplateId") != null) {
                wrapper.eq(BizfiFiReportAccountMap::getFtemplateId, query.get("ftemplateId"));
            }
            if (query.get("fitemId") != null) {
                wrapper.eq(BizfiFiReportAccountMap::getFitemId, query.get("fitemId"));
            }
            if (query.get("faccountId") != null) {
                wrapper.eq(BizfiFiReportAccountMap::getFaccountId, query.get("faccountId"));
            }
        }
        wrapper.orderByDesc(BizfiFiReportAccountMap::getFupdatetime)
                .orderByDesc(BizfiFiReportAccountMap::getFid);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }
}

