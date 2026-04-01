package single.cjj.fi.gl.init.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.init.entity.BizfiFiCashflowOpening;

import java.util.Map;

public interface BizfiFiCashflowOpeningService extends IService<BizfiFiCashflowOpening> {
    BizfiFiCashflowOpening add(BizfiFiCashflowOpening item);
    BizfiFiCashflowOpening update(BizfiFiCashflowOpening item);
    boolean delete(Long fid);
    BizfiFiCashflowOpening get(Long fid);
    IPage<BizfiFiCashflowOpening> list(int page, int size, Map<String, Object> query);
}
