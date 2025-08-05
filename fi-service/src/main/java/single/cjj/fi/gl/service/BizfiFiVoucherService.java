package single.cjj.fi.gl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.entity.BizfiFiVoucher;

import java.util.Map;

/**
 * 财务凭证服务接口
 */
public interface BizfiFiVoucherService extends IService<BizfiFiVoucher> {
    BizfiFiVoucher add(BizfiFiVoucher voucher);

    BizfiFiVoucher update(BizfiFiVoucher voucher);

    BizfiFiVoucher get(Long fid);

    IPage<BizfiFiVoucher> list(int page, int size, Map<String, Object> query);
}
