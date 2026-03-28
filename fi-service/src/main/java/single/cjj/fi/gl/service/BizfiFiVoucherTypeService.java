package single.cjj.fi.gl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.entity.BizfiFiVoucherType;

import java.util.Map;

public interface BizfiFiVoucherTypeService extends IService<BizfiFiVoucherType> {
    BizfiFiVoucherType add(BizfiFiVoucherType voucherType);

    BizfiFiVoucherType update(BizfiFiVoucherType voucherType);

    boolean delete(Long fid);

    BizfiFiVoucherType get(Long fid);

    IPage<BizfiFiVoucherType> list(int page, int size, Map<String, Object> query);

    BizfiFiVoucherType enable(Long fid);

    BizfiFiVoucherType disable(Long fid);
}
