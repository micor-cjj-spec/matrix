package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.gl.entity.BizfiFiGlEntry;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.mapper.BizfiFiGlEntryMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherLineMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.service.BizfiFiVoucherService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_REVERSED = "REVERSED";

    @Autowired
    private BizfiFiVoucherMapper mapper;

    @Autowired
    private BizfiFiVoucherLineMapper lineMapper;

    @Autowired
    private BizfiFiGlEntryMapper glEntryMapper;

    @Override
    public BizfiFiVoucher saveDraft(BizfiFiVoucher voucher) {
        validateBase(voucher);
        if (!StringUtils.hasText(voucher.getFnumber())) {
            voucher.setFnumber(generateVoucherNumber(voucher.getFdate()));
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
        if (!STATUS_DRAFT.equals(db.getFstatus()) && !STATUS_REJECTED.equals(db.getFstatus())) {
            throw new BizException("只有草稿/驳回状态可提交");
        }
        AmountSummary summary = validateAndSumLines(fid, true);
        db.setFamount(summary.debitTotal);
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
    @Transactional(rollbackFor = Exception.class)
    public BizfiFiVoucher post(Long fid, String operator) {
        BizfiFiVoucher db = mustGet(fid);
        if (!STATUS_AUDITED.equals(db.getFstatus())) {
            throw new BizException("只有已审核状态可过账");
        }

        AmountSummary summary = validateAndSumLines(fid, true);
        String postedBy = StringUtils.hasText(operator) ? operator : "system";
        LocalDateTime now = LocalDateTime.now();

        // 清理已存在过账分录（幂等）
        glEntryMapper.delete(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .eq(BizfiFiGlEntry::getFvoucherId, fid));

        List<BizfiFiVoucherLine> lines = listLines(fid);
        for (BizfiFiVoucherLine line : lines) {
            BizfiFiGlEntry entry = new BizfiFiGlEntry();
            entry.setFvoucherId(fid);
            entry.setFvoucherLineId(line.getFid());
            entry.setFvoucherNumber(db.getFnumber());
            entry.setFvoucherDate(db.getFdate());
            entry.setFaccountCode(line.getFaccountCode());
            entry.setFsummary(StringUtils.hasText(line.getFsummary()) ? line.getFsummary() : db.getFsummary());
            entry.setFdebitAmount(zeroIfNull(line.getFdebitAmount()));
            entry.setFcreditAmount(zeroIfNull(line.getFcreditAmount()));
            entry.setFpostedBy(postedBy);
            entry.setFpostedTime(now);
            glEntryMapper.insert(entry);
        }

        db.setFamount(summary.debitTotal);
        db.setFstatus(STATUS_POSTED);
        db.setFpostedBy(postedBy);
        db.setFpostedTime(now);
        mapper.updateById(db);
        return mapper.selectById(fid);
    }

    @Override
    public BizfiFiVoucher reject(Long fid, String operator) {
        BizfiFiVoucher db = mustGet(fid);
        if (!STATUS_SUBMITTED.equals(db.getFstatus())) {
            throw new BizException("只有已提交状态可驳回");
        }
        db.setFstatus(STATUS_REJECTED);
        db.setFauditedBy(StringUtils.hasText(operator) ? operator : "system");
        db.setFauditedTime(LocalDateTime.now());
        mapper.updateById(db);
        return mapper.selectById(fid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizfiFiVoucher reverse(Long fid, String operator) {
        BizfiFiVoucher db = mustGet(fid);
        if (!STATUS_POSTED.equals(db.getFstatus())) {
            throw new BizException("只有已过账状态可冲销");
        }

        String op = StringUtils.hasText(operator) ? operator : "system";
        LocalDateTime now = LocalDateTime.now();

        BizfiFiVoucher reverseVoucher = new BizfiFiVoucher();
        reverseVoucher.setFnumber("RV-" + db.getFnumber());
        reverseVoucher.setFdate(LocalDate.now());
        reverseVoucher.setFsummary("冲销:" + db.getFsummary());
        reverseVoucher.setFamount(db.getFamount());
        reverseVoucher.setFstatus(STATUS_POSTED);
        reverseVoucher.setFpostedBy(op);
        reverseVoucher.setFpostedTime(now);
        reverseVoucher.setFremark("冲销原凭证ID=" + db.getFid());
        mapper.insert(reverseVoucher);

        List<BizfiFiVoucherLine> originalLines = listLines(fid);
        for (BizfiFiVoucherLine line : originalLines) {
            BizfiFiVoucherLine reverseLine = new BizfiFiVoucherLine();
            reverseLine.setFvoucherId(reverseVoucher.getFid());
            reverseLine.setFlineNo(line.getFlineNo());
            reverseLine.setFaccountCode(line.getFaccountCode());
            reverseLine.setFsummary("冲销:" + line.getFsummary());
            reverseLine.setFdebitAmount(zeroIfNull(line.getFcreditAmount()));
            reverseLine.setFcreditAmount(zeroIfNull(line.getFdebitAmount()));
            reverseLine.setFcurrency(line.getFcurrency());
            reverseLine.setFrate(line.getFrate());
            reverseLine.setForiginalAmount(line.getForiginalAmount());
            lineMapper.insert(reverseLine);

            BizfiFiGlEntry entry = new BizfiFiGlEntry();
            entry.setFvoucherId(reverseVoucher.getFid());
            entry.setFvoucherLineId(reverseLine.getFid());
            entry.setFvoucherNumber(reverseVoucher.getFnumber());
            entry.setFvoucherDate(reverseVoucher.getFdate());
            entry.setFaccountCode(reverseLine.getFaccountCode());
            entry.setFsummary(reverseLine.getFsummary());
            entry.setFdebitAmount(zeroIfNull(reverseLine.getFdebitAmount()));
            entry.setFcreditAmount(zeroIfNull(reverseLine.getFcreditAmount()));
            entry.setFpostedBy(op);
            entry.setFpostedTime(now);
            glEntryMapper.insert(entry);
        }

        db.setFstatus(STATUS_REVERSED);
        db.setFremark((db.getFremark() == null ? "" : db.getFremark() + "；") + "已冲销到凭证:" + reverseVoucher.getFnumber());
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

    @Override
    public List<BizfiFiVoucherLine> listLines(Long voucherId) {
        return lineMapper.selectList(new LambdaQueryWrapper<BizfiFiVoucherLine>()
                .eq(BizfiFiVoucherLine::getFvoucherId, voucherId)
                .orderByAsc(BizfiFiVoucherLine::getFlineNo)
                .orderByAsc(BizfiFiVoucherLine::getFid));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveLines(Long voucherId, List<BizfiFiVoucherLine> lines) {
        BizfiFiVoucher voucher = mustGet(voucherId);
        if (!STATUS_DRAFT.equals(voucher.getFstatus()) && !STATUS_REJECTED.equals(voucher.getFstatus())) {
            throw new BizException("只有草稿/驳回状态可维护分录");
        }

        lineMapper.delete(new LambdaQueryWrapper<BizfiFiVoucherLine>()
                .eq(BizfiFiVoucherLine::getFvoucherId, voucherId));

        List<BizfiFiVoucherLine> safeLines = lines == null ? new ArrayList<>() : lines;
        int idx = 1;
        for (BizfiFiVoucherLine line : safeLines) {
            if (!StringUtils.hasText(line.getFaccountCode())) {
                throw new BizException("第" + idx + "行科目不能为空");
            }
            BigDecimal debit = zeroIfNull(line.getFdebitAmount());
            BigDecimal credit = zeroIfNull(line.getFcreditAmount());
            if (debit.signum() < 0 || credit.signum() < 0) {
                throw new BizException("第" + idx + "行借贷金额不能小于0");
            }
            if (debit.signum() == 0 && credit.signum() == 0) {
                throw new BizException("第" + idx + "行借贷金额不能同时为0");
            }
            if (debit.signum() > 0 && credit.signum() > 0) {
                throw new BizException("第" + idx + "行借贷金额不能同时大于0");
            }
            line.setFid(null);
            line.setFvoucherId(voucherId);
            line.setFlineNo(line.getFlineNo() == null ? idx : line.getFlineNo());
            lineMapper.insert(line);
            idx++;
        }
        return true;
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
        if (!StringUtils.hasText(voucher.getFsummary())) {
            throw new BizException("凭证摘要不能为空");
        }
        if (voucher.getFamount() == null || voucher.getFamount().signum() <= 0) {
            voucher.setFamount(BigDecimal.ONE);
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private AmountSummary validateAndSumLines(Long voucherId, boolean strictBalanced) {
        List<BizfiFiVoucherLine> lines = listLines(voucherId);
        if (lines == null || lines.size() < 2) {
            throw new BizException("凭证至少需要2条分录");
        }

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        int idx = 1;
        for (BizfiFiVoucherLine line : lines) {
            if (!StringUtils.hasText(line.getFaccountCode())) {
                throw new BizException("第" + idx + "行科目不能为空");
            }
            BigDecimal debit = zeroIfNull(line.getFdebitAmount());
            BigDecimal credit = zeroIfNull(line.getFcreditAmount());
            if (debit.signum() < 0 || credit.signum() < 0) {
                throw new BizException("第" + idx + "行借贷金额不能小于0");
            }
            if (debit.signum() == 0 && credit.signum() == 0) {
                throw new BizException("第" + idx + "行借贷金额不能同时为0");
            }
            if (debit.signum() > 0 && credit.signum() > 0) {
                throw new BizException("第" + idx + "行借贷金额不能同时大于0");
            }
            debitTotal = debitTotal.add(debit);
            creditTotal = creditTotal.add(credit);
            idx++;
        }

        if (strictBalanced && debitTotal.compareTo(creditTotal) != 0) {
            throw new BizException("借贷不平衡，借方=" + debitTotal + "，贷方=" + creditTotal);
        }
        return new AmountSummary(debitTotal, creditTotal);
    }

    private String generateVoucherNumber(LocalDate date) {
        LocalDate d = date == null ? LocalDate.now() : date;
        String prefix = String.format("V%s%02d", d.getYear(), d.getMonthValue());

        LambdaQueryWrapper<BizfiFiVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(BizfiFiVoucher::getFnumber, prefix)
                .orderByDesc(BizfiFiVoucher::getFnumber)
                .last("limit 1");
        BizfiFiVoucher latest = mapper.selectOne(wrapper);

        int seq = 1;
        if (latest != null && StringUtils.hasText(latest.getFnumber())) {
            String number = latest.getFnumber();
            String suffix = number.substring(Math.min(prefix.length(), number.length()));
            try {
                seq = Integer.parseInt(suffix) + 1;
            } catch (Exception ignore) {
                seq = (int) (System.currentTimeMillis() % 10000);
            }
        }
        return String.format("%s%04d", prefix, seq);
    }

    private static class AmountSummary {
        private final BigDecimal debitTotal;
        private final BigDecimal creditTotal;

        private AmountSummary(BigDecimal debitTotal, BigDecimal creditTotal) {
            this.debitTotal = debitTotal;
            this.creditTotal = creditTotal;
        }
    }
}
