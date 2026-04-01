package single.cjj.fi.gl.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.report.entity.BizfiFiReportTemplate;

import java.util.Map;

public interface BizfiFiReportTemplateService extends IService<BizfiFiReportTemplate> {
    BizfiFiReportTemplate add(BizfiFiReportTemplate template);

    BizfiFiReportTemplate update(BizfiFiReportTemplate template);

    boolean delete(Long fid);

    BizfiFiReportTemplate get(Long fid);

    IPage<BizfiFiReportTemplate> list(int page, int size, Map<String, Object> query);

    BizfiFiReportTemplate getEnabledTemplate(String type, Long templateId);

    BizfiFiReportTemplate getEnabledTemplate(String type, Long templateId, Long orgId);
}
