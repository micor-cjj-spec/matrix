package single.cjj.fi.gl.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.report.entity.BizfiFiReportAccountMap;

import java.util.Map;

public interface BizfiFiReportAccountMapService extends IService<BizfiFiReportAccountMap> {
    BizfiFiReportAccountMap add(BizfiFiReportAccountMap accountMap);

    BizfiFiReportAccountMap update(BizfiFiReportAccountMap accountMap);

    boolean delete(Long fid);

    BizfiFiReportAccountMap get(Long fid);

    IPage<BizfiFiReportAccountMap> list(int page, int size, Map<String, Object> query);
}

