package single.cjj.fi.gl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.entity.BizfiFiAccount;

import java.util.Map;

/**
 * 会计科目服务接口
 */
public interface BizfiFiAccountService extends IService<BizfiFiAccount> {
    BizfiFiAccount add(BizfiFiAccount account);

    BizfiFiAccount update(BizfiFiAccount account);

    boolean delete(Long fid);

    BizfiFiAccount get(Long fid);

    IPage<BizfiFiAccount> list(int page, int size, Map<String, Object> query);
}
