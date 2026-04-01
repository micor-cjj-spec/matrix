package single.cjj.fi.gl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.entity.BizfiFiAccountingPeriod;

import java.util.Map;

public interface BizfiFiAccountingPeriodService extends IService<BizfiFiAccountingPeriod> {
    BizfiFiAccountingPeriod add(BizfiFiAccountingPeriod period);

    BizfiFiAccountingPeriod update(BizfiFiAccountingPeriod period);

    boolean delete(Long fid);

    BizfiFiAccountingPeriod get(Long fid);

    IPage<BizfiFiAccountingPeriod> list(int page, int size, Map<String, Object> query);

    BizfiFiAccountingPeriod close(Long fid, String operator);

    BizfiFiAccountingPeriod reopen(Long fid);
}
