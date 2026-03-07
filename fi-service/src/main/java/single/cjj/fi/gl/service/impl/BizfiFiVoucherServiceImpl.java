package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.service.BizfiFiVoucherService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 财务凭证服务实现
 */
@Service
public class BizfiFiVoucherServiceImpl extends ServiceImpl<BizfiFiVoucherMapper, BizfiFiVoucher>
        implements BizfiFiVoucherService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_AUDITED = "AUDITED";
    private static final String STATUS_POSTED = "POSTED";

    @Autowired
    private BizfiFiVoucherMapper mapper;

    @Override
    public BizfiFiVoucher saveDraft(BizfiFiVoucher voucher) {
        validateBase(voucher);
        if (!StringUtils.hasText(voucher.getFnumber())) {
            voucher.setFnumber("V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
        }
        voucher.setFstatus(STATUS_DRAFT);
        mapper.insert(voucher);
        return voucher;
    }

    @Override
    public BizfiFiVoucher updateDraft(BizfiFiVoucher voucher) {
        if (voucher.getFid() == null) {
            throw new BizException("凭证ID不能为空");
        }
        BizfiFiVoucher db = get(voucher.getFid());
        if (db == null) {
            throw new BizException("凭证不存在");
        }
        if (!STATUS_DRAFT.equals(db.getFstatus())) {
            throw new BizException("只有草稿状态可修改");
        }

        validateBase(voucher);
        voucher.setFstatus(STATUS_DRAFT);
        mapper.updateById(voucher);
        return mapper.selectById(voucher.getFid());
    }

    @Override
    public BizfiFiVoucher submit(Long fid) {
        BizfiFiVoucher db = mustGet(fid);
        if (!STATUS_DRAFT.equals(db.getFstatus())) {
            throw new BizException("只有草稿状态可提交");
        }
        db.setFstatus(STATUS_SUBMITTED);
        mapper.updateById(db);
        return mapper.selectById(fid);
    }

    @Override
    public BizfiFiVoucher audit(Long fid, String operator) {
        BizfiFiVoucher db = mustGet(fid);
        if (!STATUS_SUBMITTED.equals(db.getFstatus())) {
            throw new BizException("只有已提交状态可审核");
        }
        db.setFstatus(STATUS_AUDITED);
        db.setFauditedBy(StringUtils.hasText(operator) ? operator : "system");
        db.setFauditedTime(LocalDateTime.now());
        mapper.updateById(db);
        return mapper.selectById(fid);
    }

    @Override
    public BizfiFiVoucher post(Long fid, String operator) {
        BizfiFiVoucher db = mustGet(fid);
        if (!STATUS_AUDITED.equals(db.getFstatus())) {
            throw new BizException("只有已审核状态可过账");
        }
        db.setFstatus(STATUS_POSTED);
        db.setFpostedBy(StringUtils.hasText(operator) ? operator : "system");
        db.setFpostedTime(LocalDateTime.now());
        mapper.updateById(db);
        return mapper.selectById(fid);
    }

    @Override
    public BizfiFiVoucher get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public Boolean deleteDraft(Long fid) {
        BizfiFiVoucher db = mustGet(fid);
        if (!STATUS_DRAFT.equals(db.getFstatus())) {
            throw new BizException("只有草稿状态可删除");
        }
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public IPage<BizfiFiVoucher> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiVoucher> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText((String) query.get("number"))) {
                wrapper.like(BizfiFiVoucher::getFnumber, query.get("number"));
            }
            if (StringUtils.hasText((String) query.get("summary"))) {
                wrapper.like(BizfiFiVoucher::getFsummary, query.get("summary"));
            }
            if (StringUtils.hasText((String) query.get("status"))) {
                wrapper.eq(BizfiFiVoucher::getFstatus, query.get("status"));
            }
        }
        wrapper.orderByDesc(BizfiFiVoucher::getFdate).orderByDesc(BizfiFiVoucher::getFid);
        Page<BizfiFiVoucher> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }

    private BizfiFiVoucher mustGet(Long fid) {
        BizfiFiVoucher db = get(fid);
        if (db == null) {
            throw new BizException("凭证不存在");
        }
        return db;
    }

    private void validateBase(BizfiFiVoucher voucher) {
        if (voucher == null) {
            throw new BizException("凭证参数不能为空");
        }
        if (voucher.getFdate() == null) {
            throw new BizException("凭证日期不能为空");
        }
        if (voucher.getFamount() == null || voucher.getFamount().signum() <= 0) {
            throw new BizException("凭证金额必须大于0");
        }
    }
}
