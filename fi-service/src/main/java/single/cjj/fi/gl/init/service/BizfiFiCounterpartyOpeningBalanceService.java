package single.cjj.fi.gl.init.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.init.entity.BizfiFiCounterpartyOpeningBalance;

import java.util.Map;

public interface BizfiFiCounterpartyOpeningBalanceService extends IService<BizfiFiCounterpartyOpeningBalance> {
    BizfiFiCounterpartyOpeningBalance add(BizfiFiCounterpartyOpeningBalance item);
    BizfiFiCounterpartyOpeningBalance update(BizfiFiCounterpartyOpeningBalance item);
    boolean delete(Long fid);
    BizfiFiCounterpartyOpeningBalance get(Long fid);
    IPage<BizfiFiCounterpartyOpeningBalance> list(int page, int size, Map<String, Object> query);
}
