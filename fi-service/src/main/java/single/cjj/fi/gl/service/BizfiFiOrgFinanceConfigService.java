package single.cjj.fi.gl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.entity.BizfiFiOrgFinanceConfig;

import java.util.Map;

public interface BizfiFiOrgFinanceConfigService extends IService<BizfiFiOrgFinanceConfig> {
    BizfiFiOrgFinanceConfig add(BizfiFiOrgFinanceConfig config);

    BizfiFiOrgFinanceConfig update(BizfiFiOrgFinanceConfig config);

    boolean delete(Long fid);

    BizfiFiOrgFinanceConfig get(Long fid);

    BizfiFiOrgFinanceConfig getByOrg(Long forg);

    IPage<BizfiFiOrgFinanceConfig> list(int page, int size, Map<String, Object> query);
}
