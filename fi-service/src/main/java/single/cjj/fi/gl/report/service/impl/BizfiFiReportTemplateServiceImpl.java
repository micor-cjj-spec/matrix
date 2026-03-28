package single.cjj.fi.gl.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.report.entity.BizfiFiReportTemplate;
import single.cjj.fi.gl.report.mapper.BizfiFiReportTemplateMapper;
import single.cjj.fi.gl.report.service.BizfiFiReportTemplateService;

import java.util.Map;

@Service
public class BizfiFiReportTemplateServiceImpl
        extends ServiceImpl<BizfiFiReportTemplateMapper, BizfiFiReportTemplate>
        implements BizfiFiReportTemplateService {

    @Autowired
    private BizfiFiReportTemplateMapper mapper;

    @Override
    public BizfiFiReportTemplate add(BizfiFiReportTemplate template) {
        mapper.insert(template);
        return template;
    }

    @Override
    public BizfiFiReportTemplate update(BizfiFiReportTemplate template) {
        mapper.updateById(template);
        return mapper.selectById(template.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiReportTemplate get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiReportTemplate> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiReportTemplate> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText((String) query.get("fcode"))) {
                wrapper.like(BizfiFiReportTemplate::getFcode, query.get("fcode"));
            }
            if (StringUtils.hasText((String) query.get("fname"))) {
                wrapper.like(BizfiFiReportTemplate::getFname, query.get("fname"));
            }
            if (StringUtils.hasText((String) query.get("ftype"))) {
                wrapper.eq(BizfiFiReportTemplate::getFtype, query.get("ftype"));
            }
            if (StringUtils.hasText((String) query.get("fstatus"))) {
                wrapper.eq(BizfiFiReportTemplate::getFstatus, query.get("fstatus"));
            }
            if (query.get("forg") != null) {
                wrapper.eq(BizfiFiReportTemplate::getForg, query.get("forg"));
            }
        }
        wrapper.orderByDesc(BizfiFiReportTemplate::getFupdatetime)
                .orderByDesc(BizfiFiReportTemplate::getFid);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public BizfiFiReportTemplate getEnabledTemplate(String type, Long templateId) {
        return getEnabledTemplate(type, templateId, null);
    }

    @Override
    public BizfiFiReportTemplate getEnabledTemplate(String type, Long templateId, Long orgId) {
        if (templateId != null) {
            BizfiFiReportTemplate template = mapper.selectById(templateId);
            if (template == null) {
                return null;
            }
            if (!"ENABLED".equalsIgnoreCase(template.getFstatus())) {
                return null;
            }
            if (StringUtils.hasText(type) && !type.equalsIgnoreCase(template.getFtype())) {
                return null;
            }
            return template;
        }

        if (orgId != null) {
            BizfiFiReportTemplate orgTemplate = mapper.selectOne(new LambdaQueryWrapper<BizfiFiReportTemplate>()
                    .eq(BizfiFiReportTemplate::getFstatus, "ENABLED")
                    .eq(BizfiFiReportTemplate::getForg, orgId)
                    .eq(StringUtils.hasText(type), BizfiFiReportTemplate::getFtype, type)
                    .orderByDesc(BizfiFiReportTemplate::getFupdatetime)
                    .orderByDesc(BizfiFiReportTemplate::getFid)
                    .last("limit 1"));
            if (orgTemplate != null) {
                return orgTemplate;
            }

            BizfiFiReportTemplate globalTemplate = mapper.selectOne(new LambdaQueryWrapper<BizfiFiReportTemplate>()
                    .eq(BizfiFiReportTemplate::getFstatus, "ENABLED")
                    .isNull(BizfiFiReportTemplate::getForg)
                    .eq(StringUtils.hasText(type), BizfiFiReportTemplate::getFtype, type)
                    .orderByDesc(BizfiFiReportTemplate::getFupdatetime)
                    .orderByDesc(BizfiFiReportTemplate::getFid)
                    .last("limit 1"));
            if (globalTemplate != null) {
                return globalTemplate;
            }
        }

        LambdaQueryWrapper<BizfiFiReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizfiFiReportTemplate::getFstatus, "ENABLED");
        if (StringUtils.hasText(type)) {
            wrapper.eq(BizfiFiReportTemplate::getFtype, type);
        }
        wrapper.orderByDesc(BizfiFiReportTemplate::getFupdatetime)
                .orderByDesc(BizfiFiReportTemplate::getFid)
                .last("limit 1");
        return mapper.selectOne(wrapper);
    }
}
