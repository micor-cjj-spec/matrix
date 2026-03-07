package single.cjj.bizfi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.entity.BizfiBaseOrgPattern;
import single.cjj.bizfi.mapper.BizfiBaseOrgPatternMapper;
import single.cjj.bizfi.service.BizfiBaseOrgPatternService;

import java.util.Map;

/**
 * 组织形态服务实现
 */
@Service
public class BizfiBaseOrgPatternServiceImpl extends ServiceImpl<BizfiBaseOrgPatternMapper, BizfiBaseOrgPattern>
        implements BizfiBaseOrgPatternService {

    @Autowired
    private BizfiBaseOrgPatternMapper mapper;

    @Override
    public BizfiBaseOrgPattern add(BizfiBaseOrgPattern pattern) {
        mapper.insert(pattern);
        return pattern;
    }

    @Override
    public BizfiBaseOrgPattern update(BizfiBaseOrgPattern pattern) {
        mapper.updateById(pattern);
        return mapper.selectById(pattern.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiBaseOrgPattern get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiBaseOrgPattern> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiBaseOrgPattern> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText((String) query.get("fname"))) {
                wrapper.like(BizfiBaseOrgPattern::getFname, query.get("fname"));
            }
            if (StringUtils.hasText((String) query.get("fbillno"))) {
                wrapper.like(BizfiBaseOrgPattern::getFbillno, query.get("fbillno"));
            }
            if (StringUtils.hasText((String) query.get("fpattern"))) {
                wrapper.eq(BizfiBaseOrgPattern::getFpattern, query.get("fpattern"));
            }
            if (query.get("fenable") != null) {
                wrapper.eq(BizfiBaseOrgPattern::getFenable, query.get("fenable"));
            }
        }
        Page<BizfiBaseOrgPattern> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }
}
