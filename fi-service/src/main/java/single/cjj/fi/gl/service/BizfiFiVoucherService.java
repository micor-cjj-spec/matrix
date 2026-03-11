package single.cjj.fi.gl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;

import java.util.List;
import java.util.Map;

/**
 * 财务凭证服务接口
 */
public interface BizfiFiVoucherService extends IService<BizfiFiVoucher> {

    BizfiFiVoucher saveDraft(BizfiFiVoucher voucher);

    BizfiFiVoucher updateDraft(BizfiFiVoucher voucher);

    BizfiFiVoucher submit(Long fid);

    BizfiFiVoucher audit(Long fid, String operator);

    BizfiFiVoucher post(Long fid, String operator);

    BizfiFiVoucher reject(Long fid, String operator);

    BizfiFiVoucher reverse(Long fid, String operator);

    BizfiFiVoucher get(Long fid);

    Boolean deleteDraft(Long fid);

    IPage<BizfiFiVoucher> list(int page, int size, Map<String, Object> query);

    List<BizfiFiVoucherLine> listLines(Long voucherId);

    Boolean saveLines(Long voucherId, List<BizfiFiVoucherLine> lines);
}
