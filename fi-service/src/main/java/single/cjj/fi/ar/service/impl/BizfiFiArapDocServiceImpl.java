package single.cjj.fi.ar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.mapper.BizfiFiArapDocMapper;
import single.cjj.fi.ar.service.BizfiFiArapDocService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class BizfiFiArapDocServiceImpl implements BizfiFiArapDocService {

    private static final String DRAFT = "DRAFT";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String AUDITED = "AUDITED";
    private static final String REJECTED = "REJECTED";

    @Autowired
    private BizfiFiArapDocMapper mapper;

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
    public BizfiFiArapDoc audit(Long fid, String operator) {
        BizfiFiArapDoc db = mustGet(fid);
        if (!SUBMITTED.equals(db.getFstatus())) throw new BizException("仅已提交可审核");
        db.setFstatus(AUDITED);
        db.setFauditedBy(StringUtils.hasText(operator) ? operator : "system");
        db.setFauditedTime(LocalDateTime.now());
        mapper.updateById(db);
        return mapper.selectById(fid);
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
    public IPage<BizfiFiArapDoc> list(String docType, int page, int size, String number, String status) {
        LambdaQueryWrapper<BizfiFiArapDoc> w = new LambdaQueryWrapper<>();
        w.eq(BizfiFiArapDoc::getFdoctype, docType);
        if (StringUtils.hasText(number)) w.like(BizfiFiArapDoc::getFnumber, number);
        if (StringUtils.hasText(status)) w.eq(BizfiFiArapDoc::getFstatus, status);
        w.orderByDesc(BizfiFiArapDoc::getFdate).orderByDesc(BizfiFiArapDoc::getFid);
        return mapper.selectPage(new Page<>(page, size), w);
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
