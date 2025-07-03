package single.cjj.bizfi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.entity.BizfiBaseOrgFunctionType;
import single.cjj.bizfi.mapper.BizfiBaseOrgFunctionTypeMapper;
import single.cjj.bizfi.service.BizfiBaseOrgFunctionTypeService;

import java.util.Map;

/**
 * 组织职能类型服务实现
 */
@Service
public class BizfiBaseOrgFunctionTypeServiceImpl extends ServiceImpl<BizfiBaseOrgFunctionTypeMapper, BizfiBaseOrgFunctionType> implements BizfiBaseOrgFunctionTypeService {
    @Autowired
    private BizfiBaseOrgFunctionTypeMapper mapper;

    @Override
    public BizfiBaseOrgFunctionType add(BizfiBaseOrgFunctionType type) {
        mapper.insert(type);
        return type;
    }

    @Override
    public BizfiBaseOrgFunctionType update(BizfiBaseOrgFunctionType type) {
        mapper.updateById(type);
        return mapper.selectById(type.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiBaseOrgFunctionType get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiBaseOrgFunctionType> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiBaseOrgFunctionType> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText((String) query.get("fname"))) {
                wrapper.like(BizfiBaseOrgFunctionType::getFname, query.get("fname"));
            }
            if (StringUtils.hasText((String) query.get("fnumber"))) {
                wrapper.like(BizfiBaseOrgFunctionType::getFnumber, query.get("fnumber"));
            }
            if (StringUtils.hasText((String) query.get("ftype"))) {
                wrapper.eq(BizfiBaseOrgFunctionType::getFtype, query.get("ftype"));
            }
            if (query.get("fbasefunction") != null) {
                wrapper.eq(BizfiBaseOrgFunctionType::getFbasefunction, query.get("fbasefunction"));
            }
            if (query.get("fcustom") != null) {
                wrapper.eq(BizfiBaseOrgFunctionType::getFcustom, query.get("fcustom"));
            }
        }
        Page<BizfiBaseOrgFunctionType> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }
}
