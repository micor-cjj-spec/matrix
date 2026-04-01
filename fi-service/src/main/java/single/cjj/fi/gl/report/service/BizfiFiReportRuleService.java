package single.cjj.fi.gl.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.report.entity.BizfiFiReportRule;

import java.util.Map;

public interface BizfiFiReportRuleService extends IService<BizfiFiReportRule> {
    BizfiFiReportRule add(BizfiFiReportRule rule);

    BizfiFiReportRule update(BizfiFiReportRule rule);

    boolean delete(Long fid);

    BizfiFiReportRule get(Long fid);

    IPage<BizfiFiReportRule> list(int page, int size, Map<String, Object> query);
}

