package single.cjj.fi.gl.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.report.entity.BizfiFiReportItem;

import java.util.List;
import java.util.Map;

public interface BizfiFiReportItemService extends IService<BizfiFiReportItem> {
    BizfiFiReportItem add(BizfiFiReportItem item);

    BizfiFiReportItem update(BizfiFiReportItem item);

    boolean delete(Long fid);

    BizfiFiReportItem get(Long fid);

    IPage<BizfiFiReportItem> list(int page, int size, Map<String, Object> query);

    List<BizfiFiReportItem> listByTemplateId(Long templateId);
}

