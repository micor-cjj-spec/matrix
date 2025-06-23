package single.cjj.bizfi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.entity.BizfiDeptDim;
import single.cjj.bizfi.mapper.BizfiDeptDimMapper;
import single.cjj.bizfi.service.BizfiDeptDimService;

import java.util.Map;

/**
 * 部门维度服务实现
 */
@Service
public class BizfiDeptDimServiceImpl extends ServiceImpl<BizfiDeptDimMapper, BizfiDeptDim> implements BizfiDeptDimService {
    @Autowired
    private BizfiDeptDimMapper mapper;

    @Override
    public BizfiDeptDim add(BizfiDeptDim dept) {
        mapper.insert(dept);
        return dept;
    }

    @Override
    public BizfiDeptDim update(BizfiDeptDim dept) {
        mapper.updateById(dept);
        return mapper.selectById(dept.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiDeptDim get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiDeptDim> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiDeptDim> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText((String) query.get("fname"))) {
                wrapper.like(BizfiDeptDim::getFname, query.get("fname"));
            }
            if (StringUtils.hasText((String) query.get("fcode"))) {
                wrapper.like(BizfiDeptDim::getFcode, query.get("fcode"));
            }
            if (query.get("fbizunitid") != null) {
                wrapper.eq(BizfiDeptDim::getFbizunitid, query.get("fbizunitid"));
            }
            if (StringUtils.hasText((String) query.get("fstatus"))) {
                wrapper.eq(BizfiDeptDim::getFstatus, query.get("fstatus"));
            }
        }
        Page<BizfiDeptDim> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }
}
