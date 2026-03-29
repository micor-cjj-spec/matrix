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
import single.cjj.bizfi.util.TextEncodingFixer;

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
        return sanitize(unit);
    }

    @Override
    public BizfiBizUnit update(BizfiBizUnit unit) {
        mapper.updateById(unit);
        return sanitize(mapper.selectById(unit.getFid()));
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiBizUnit get(Long fid) {
        return sanitize(mapper.selectById(fid));
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
            if (StringUtils.hasText((String) query.get("fusagestatus"))) {
                wrapper.eq(BizfiBizUnit::getFusagestatus, query.get("fusagestatus"));
            }
        }
        wrapper.orderByAsc(BizfiBizUnit::getFcode);
        Page<BizfiBizUnit> pageObj = new Page<>(page, size);
        IPage<BizfiBizUnit> result = mapper.selectPage(pageObj, wrapper);
        result.getRecords().replaceAll(this::sanitize);
        return result;
    }

    private BizfiBizUnit sanitize(BizfiBizUnit unit) {
        if (unit == null) {
            return null;
        }
        unit.setFname(TextEncodingFixer.fix(unit.getFname()));
        unit.setFshortName(TextEncodingFixer.fix(unit.getFshortName()));
        unit.setFform(TextEncodingFixer.fix(unit.getFform()));
        unit.setFformType(TextEncodingFixer.fix(unit.getFformType()));
        unit.setFdescription(TextEncodingFixer.fix(unit.getFdescription()));
        unit.setFmanageOrgName(TextEncodingFixer.fix(unit.getFmanageOrgName()));
        unit.setFinvestmentType(TextEncodingFixer.fix(unit.getFinvestmentType()));
        unit.setFindustryName(TextEncodingFixer.fix(unit.getFindustryName()));
        unit.setFregionArea(TextEncodingFixer.fix(unit.getFregionArea()));
        unit.setFarea(TextEncodingFixer.fix(unit.getFarea()));
        unit.setFdefaultLegalEntity(TextEncodingFixer.fix(unit.getFdefaultLegalEntity()));
        unit.setFaccountingOrg(TextEncodingFixer.fix(unit.getFaccountingOrg()));
        unit.setFmanageOrgType(TextEncodingFixer.fix(unit.getFmanageOrgType()));
        unit.setFlegalEntityStatus(TextEncodingFixer.fix(unit.getFlegalEntityStatus()));
        unit.setFactualControlType(TextEncodingFixer.fix(unit.getFactualControlType()));
        unit.setFincomeCostType(TextEncodingFixer.fix(unit.getFincomeCostType()));
        unit.setFlegalEntityCategory(TextEncodingFixer.fix(unit.getFlegalEntityCategory()));
        unit.setForgStructure(TextEncodingFixer.fix(unit.getForgStructure()));
        unit.setFusagestatus(TextEncodingFixer.fix(unit.getFusagestatus()));
        unit.setFdataStatus(TextEncodingFixer.fix(unit.getFdataStatus()));
        unit.setFfunctionType(TextEncodingFixer.fix(unit.getFfunctionType()));
        unit.setFcountry(TextEncodingFixer.fix(unit.getFcountry()));
        unit.setFcity(TextEncodingFixer.fix(unit.getFcity()));
        unit.setFcontactAddress(TextEncodingFixer.fix(unit.getFcontactAddress()));
        unit.setFcompanyName(TextEncodingFixer.fix(unit.getFcompanyName()));
        unit.setFcompanyType(TextEncodingFixer.fix(unit.getFcompanyType()));
        unit.setFlegalRepresentative(TextEncodingFixer.fix(unit.getFlegalRepresentative()));
        unit.setFdomicile(TextEncodingFixer.fix(unit.getFdomicile()));
        unit.setFbusinessTerm(TextEncodingFixer.fix(unit.getFbusinessTerm()));
        unit.setFbusinessScope(TextEncodingFixer.fix(unit.getFbusinessScope()));
        unit.setFtaxpayerType(TextEncodingFixer.fix(unit.getFtaxpayerType()));
        unit.setFtaxMethod(TextEncodingFixer.fix(unit.getFtaxMethod()));
        return unit;
    }
}
