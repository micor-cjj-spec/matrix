package single.cjj.fi.ar.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;

public interface BizfiFiArapDocService {
    BizfiFiArapDoc create(BizfiFiArapDoc doc);
    BizfiFiArapDoc update(BizfiFiArapDoc doc);
    Boolean deleteDraft(Long fid);
    BizfiFiArapDoc submit(Long fid);
    BizfiFiArapDoc audit(Long fid, String operator);
    BizfiFiArapDoc reject(Long fid, String operator);
    BizfiFiArapDoc detail(Long fid);
    IPage<BizfiFiArapDoc> list(String docType, int page, int size, String number, String status);
}
