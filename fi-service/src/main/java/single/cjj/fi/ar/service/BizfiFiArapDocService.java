package single.cjj.fi.ar.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;

import java.math.BigDecimal;
import java.util.List;

public interface BizfiFiArapDocService {
    BizfiFiArapDoc create(BizfiFiArapDoc doc);
    BizfiFiArapDoc update(BizfiFiArapDoc doc);
    Boolean deleteDraft(Long fid);
    BizfiFiArapDoc submit(Long fid);
    BizfiFiArapDoc submitByNumber(String number);
    BizfiFiArapDoc audit(Long fid, String operator);
    BizfiFiArapDoc auditByNumber(String number, String operator);
    BizfiFiArapDoc reject(Long fid, String operator);
    BizfiFiArapDoc rejectByNumber(String number, String operator);
    BizfiFiArapDoc detail(Long fid);
    IPage<BizfiFiArapDoc> list(String docType, int page, int size, String number, String status,
                               String counterparty, String startDate, String endDate,
                               BigDecimal minAmount, BigDecimal maxAmount);

    BizfiFiArapDoc generateVoucher(Long fid, String operator);

    BizfiFiArapDoc generateVoucherByNumber(String number, String operator);

    List<BizfiFiArapDoc> listByVoucher(Long voucherId, String voucherNumber);
}
