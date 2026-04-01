package single.cjj.fi.gl.config.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.config.entity.BizfiFiBaseConfigItem;

import java.util.Map;

public interface BizfiFiBaseConfigItemService extends IService<BizfiFiBaseConfigItem> {
    BizfiFiBaseConfigItem add(BizfiFiBaseConfigItem item);
    BizfiFiBaseConfigItem update(BizfiFiBaseConfigItem item);
    boolean delete(Long fid);
    BizfiFiBaseConfigItem get(Long fid);
    IPage<BizfiFiBaseConfigItem> list(int page, int size, Map<String, Object> query);
}
