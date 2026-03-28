package single.cjj.fi.gl.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.report.entity.BizfiFiReportRule;
import single.cjj.fi.gl.report.mapper.BizfiFiReportRuleMapper;
import single.cjj.fi.gl.report.service.BizfiFiReportRuleService;

import java.util.Map;

@Service
public class BizfiFiReportRuleServiceImpl
        extends ServiceImpl<BizfiFiReportRuleMapper, BizfiFiReportRule>
        implements BizfiFiReportRuleService {

    @Autowired
    private BizfiFiReportRuleMapper mapper;

    @Override
    public BizfiFiReportRule add(BizfiFiReportRule rule) {
        mapper.insert(rule);
        return rule;
    }

    @Override
    public BizfiFiReportRule update(BizfiFiReportRule rule) {
        mapper.updateById(rule);
        return mapper.selectById(rule.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiReportRule get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiReportRule> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiReportRule> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.get("fitemId") != null) {
                wrapper.eq(BizfiFiReportRule::getFitemId, query.get("fitemId"));
            }
            if (StringUtils.hasText((String) query.get("fruleType"))) {
                wrapper.eq(BizfiFiReportRule::getFruleType, query.get("fruleType"));
            }
            if (StringUtils.hasText((String) query.get("fstatus"))) {
                wrapper.eq(BizfiFiReportRule::getFstatus, query.get("fstatus"));
            }
        }
        wrapper.orderByAsc(BizfiFiReportRule::getFpriority)
                .orderByAsc(BizfiFiReportRule::getFid);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }
}

