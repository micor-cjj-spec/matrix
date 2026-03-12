package single.cjj.fi.ar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.mapper.BizfiFiArapDocMapper;
import single.cjj.fi.ar.service.BizfiFiArapDocService;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.mapper.BizfiFiVoucherLineMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BizfiFiArapDocServiceImpl implements BizfiFiArapDocService {

    private static final String DRAFT = "DRAFT";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String AUDITED = "AUDITED";
    private static final String REJECTED = "REJECTED";

    private static final Map<String, String[]> DOC_VOUCHER_ACCOUNT_MAP = new HashMap<>();
    static {
        // [debitAccount, creditAccount]
        DOC_VOUCHER_ACCOUNT_MAP.put("AR", new String[]{"1122", "6001"});
        DOC_VOUCHER_ACCOUNT_MAP.put("AR_ESTIMATE", new String[]{"1122", "2202"});
        DOC_VOUCHER_ACCOUNT_MAP.put("AR_SETTLEMENT", new String[]{"1002", "1122"});

        DOC_VOUCHER_ACCOUNT_MAP.put("AP", new String[]{"6001", "2202"});
        DOC_VOUCHER_ACCOUNT_MAP.put("AP_ESTIMATE", new String[]{"1405", "2202"});
        DOC_VOUCHER_ACCOUNT_MAP.put("AP_PAYMENT_APPLY", new String[]{"2202", "2241"});
        DOC_VOUCHER_ACCOUNT_MAP.put("AP_PAYMENT_PROCESS", new String[]{"2241", "1002"});
    }

    @Autowired
    private BizfiFiArapDocMapper mapper;

    @Autowired
    private BizfiFiVoucherMapper voucherMapper;

    @Autowired
    private BizfiFiVoucherLineMapper voucherLineMapper;

    @Override
    public BizfiFiArapDoc create(BizfiFiArapDoc doc) {
        validate(doc);
        if (!StringUtils.hasText(doc.getFnumber())) {
            doc.setFnumber(doc.getFdoctype() + "-" + System.currentTimeMillis());
        }
        doc.setFstatus(DRAFT);
        mapper.insert(doc);
        return doc;
    }

    @Override
    public BizfiFiArapDoc update(BizfiFiArapDoc doc) {
        BizfiFiArapDoc db = mustGet(doc.getFid());
        if (!DRAFT.equals(db.getFstatus()) && !REJECTED.equals(db.getFstatus())) throw new BizException("仅草稿/驳回可编辑");
        validate(doc);
        doc.setFstatus(db.getFstatus());
        mapper.updateById(doc);
        return mapper.selectById(doc.getFid());
    }

    @Override
    public Boolean deleteDraft(Long fid) {
        BizfiFiArapDoc db = mustGet(fid);
        if (!DRAFT.equals(db.getFstatus())) throw new BizException("仅草稿可删除");
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiArapDoc submit(Long fid) {
        BizfiFiArapDoc db = mustGet(fid);
        if (!DRAFT.equals(db.getFstatus()) && !REJECTED.equals(db.getFstatus())) throw new BizException("仅草稿/驳回可提交");
        db.setFstatus(SUBMITTED);
        mapper.updateById(db);
        return mapper.selectById(fid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizfiFiArapDoc audit(Long fid, String operator) {
        BizfiFiArapDoc db = mustGet(fid);
        if (!SUBMITTED.equals(db.getFstatus())) throw new BizException("仅已提交可审核");
        db.setFstatus(AUDITED);
        db.setFauditedBy(StringUtils.hasText(operator) ? operator : "system");
        db.setFauditedTime(LocalDateTime.now());
        mapper.updateById(db);
        // 审核通过后自动生成并关联凭证；若已存在关联则幂等跳过
        return generateVoucher(fid, operator);
    }

    @Override
    public BizfiFiArapDoc reject(Long fid, String operator) {
        BizfiFiArapDoc db = mustGet(fid);
        if (!SUBMITTED.equals(db.getFstatus())) throw new BizException("仅已提交可驳回");
        db.setFstatus(REJECTED);
        db.setFauditedBy(StringUtils.hasText(operator) ? operator : "system");
        db.setFauditedTime(LocalDateTime.now());
        mapper.updateById(db);
        return mapper.selectById(fid);
    }

    @Override
    public BizfiFiArapDoc detail(Long fid) { return mapper.selectById(fid); }

    @Override
    public IPage<BizfiFiArapDoc> list(String docType, int page, int size, String number, String status,
                                      String counterparty, String startDate, String endDate,
                                      BigDecimal minAmount, BigDecimal maxAmount) {
        LambdaQueryWrapper<BizfiFiArapDoc> w = new LambdaQueryWrapper<>();
        w.eq(BizfiFiArapDoc::getFdoctype, docType);
        if (StringUtils.hasText(number)) w.like(BizfiFiArapDoc::getFnumber, number);
        if (StringUtils.hasText(status)) w.eq(BizfiFiArapDoc::getFstatus, status);
        if (StringUtils.hasText(counterparty)) w.like(BizfiFiArapDoc::getFcounterparty, counterparty);
        if (StringUtils.hasText(startDate)) w.ge(BizfiFiArapDoc::getFdate, startDate);
        if (StringUtils.hasText(endDate)) w.le(BizfiFiArapDoc::getFdate, endDate);
        if (minAmount != null) w.ge(BizfiFiArapDoc::getFamount, minAmount);
        if (maxAmount != null) w.le(BizfiFiArapDoc::getFamount, maxAmount);
        w.orderByDesc(BizfiFiArapDoc::getFdate).orderByDesc(BizfiFiArapDoc::getFid);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public List<BizfiFiArapDoc> listByVoucher(Long voucherId, String voucherNumber) {
        if (voucherId == null && !StringUtils.hasText(voucherNumber)) {
            throw new BizException("凭证ID或凭证号至少传一个");
        }
        LambdaQueryWrapper<BizfiFiArapDoc> w = new LambdaQueryWrapper<>();
        if (voucherId != null) {
            w.eq(BizfiFiArapDoc::getFvoucherId, voucherId);
        }
        if (StringUtils.hasText(voucherNumber)) {
            w.eq(BizfiFiArapDoc::getFvoucherNumber, voucherNumber);
        }
        w.orderByDesc(BizfiFiArapDoc::getFid);
        return mapper.selectList(w);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizfiFiArapDoc generateVoucher(Long fid, String operator) {
        BizfiFiArapDoc doc = mustGet(fid);
        return generateVoucherForDoc(doc, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizfiFiArapDoc generateVoucherByNumber(String number, String operator) {
        if (!StringUtils.hasText(number)) {
            throw new BizException("单据号不能为空");
        }
        LambdaQueryWrapper<BizfiFiArapDoc> q = new LambdaQueryWrapper<>();
        q.eq(BizfiFiArapDoc::getFnumber, number.trim()).last("limit 1");
        BizfiFiArapDoc doc = mapper.selectOne(q);
        if (doc == null) {
            throw new BizException("单据不存在");
        }
        return generateVoucherForDoc(doc, operator);
    }

    private BizfiFiArapDoc generateVoucherForDoc(BizfiFiArapDoc doc, String operator) {
        if (!AUDITED.equals(doc.getFstatus())) {
            throw new BizException("仅已审核单据可生成凭证");
        }
        if (doc.getFvoucherId() != null) {
            return mapper.selectById(doc.getFid());
        }

        String op = StringUtils.hasText(operator) ? operator : "system";
        String docType = doc.getFdoctype() == null ? "" : doc.getFdoctype().trim().toUpperCase();
        String[] accountPair = DOC_VOUCHER_ACCOUNT_MAP.get(docType);
        if (accountPair == null) {
            throw new BizException("未配置单据类型的凭证科目映射: " + docType);
        }

        BizfiFiVoucher voucher = new BizfiFiVoucher();
        voucher.setFnumber("ARAP-" + doc.getFnumber());
        voucher.setFdate(doc.getFdate() == null ? LocalDate.now() : doc.getFdate());
        voucher.setFsummary("单据转凭证:" + doc.getFnumber());
        voucher.setFamount(doc.getFamount());
        voucher.setFstatus("DRAFT");
        voucher.setFcreatedBy(op);
        voucher.setFcreatedTime(LocalDateTime.now());
        voucherMapper.insert(voucher);

        BigDecimal amt = doc.getFamount();
        BizfiFiVoucherLine l1 = new BizfiFiVoucherLine();
        l1.setFvoucherId(voucher.getFid());
        l1.setFlineNo(1);
        l1.setFaccountCode(accountPair[0]);
        l1.setFsummary(doc.getFremark());
        l1.setFdebitAmount(amt);
        l1.setFcreditAmount(BigDecimal.ZERO);
        voucherLineMapper.insert(l1);

        BizfiFiVoucherLine l2 = new BizfiFiVoucherLine();
        l2.setFvoucherId(voucher.getFid());
        l2.setFlineNo(2);
        l2.setFaccountCode(accountPair[1]);
        l2.setFsummary(doc.getFremark());
        l2.setFdebitAmount(BigDecimal.ZERO);
        l2.setFcreditAmount(amt);
        voucherLineMapper.insert(l2);

        doc.setFvoucherId(voucher.getFid());
        doc.setFvoucherNumber(voucher.getFnumber());
        mapper.updateById(doc);
        return mapper.selectById(doc.getFid());
    }

    private BizfiFiArapDoc mustGet(Long fid) {
        BizfiFiArapDoc db = mapper.selectById(fid);
        if (db == null) throw new BizException("单据不存在");
        return db;
    }

    private void validate(BizfiFiArapDoc doc) {
        if (doc == null) throw new BizException("参数不能为空");
        if (!StringUtils.hasText(doc.getFdoctype())) throw new BizException("单据类型不能为空");
        if (doc.getFdate() == null) doc.setFdate(LocalDate.now());
        if (doc.getFamount() == null || doc.getFamount().compareTo(BigDecimal.ZERO) <= 0) throw new BizException("金额必须大于0");
        if (!StringUtils.hasText(doc.getFcounterparty())) throw new BizException("往来方不能为空");
    }
}
