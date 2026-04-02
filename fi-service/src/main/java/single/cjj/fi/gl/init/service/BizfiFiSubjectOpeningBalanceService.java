package single.cjj.fi.gl.init.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.init.entity.BizfiFiSubjectOpeningBalance;

import java.util.Map;

public interface BizfiFiSubjectOpeningBalanceService extends IService<BizfiFiSubjectOpeningBalance> {
    BizfiFiSubjectOpeningBalance add(BizfiFiSubjectOpeningBalance item);
    BizfiFiSubjectOpeningBalance update(BizfiFiSubjectOpeningBalance item);
    boolean delete(Long fid);
    BizfiFiSubjectOpeningBalance get(Long fid);
    IPage<BizfiFiSubjectOpeningBalance> list(int page, int size, Map<String, Object> query);
}
