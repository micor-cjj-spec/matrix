package single.cjj.fi.gl.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.report.entity.BizfiFiCashflowItem;

import java.util.Map;

public interface BizfiFiCashflowItemService extends IService<BizfiFiCashflowItem> {
    BizfiFiCashflowItem add(BizfiFiCashflowItem item);

    BizfiFiCashflowItem update(BizfiFiCashflowItem item);

    boolean delete(Long fid);

    BizfiFiCashflowItem get(Long fid);

    IPage<BizfiFiCashflowItem> list(int page, int size, Map<String, Object> query);
}

