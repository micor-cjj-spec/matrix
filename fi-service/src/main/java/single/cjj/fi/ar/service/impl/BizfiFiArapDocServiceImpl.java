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
import single.cjj.fi.ar.entity.BizfiFiCounterpartyCredit;
import single.cjj.fi.ar.mapper.BizfiFiArapDocMapper;
import single.cjj.fi.ar.mapper.BizfiFiCounterpartyCreditMapper;
import single.cjj.fi.ar.service.BizfiFiArapDocService;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.mapper.BizfiFiVoucherLineMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
    private BizfiFiCounterpartyCreditMapper creditMapper;

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
        return doSubmit(db);
    }

    @Override
    public BizfiFiArapDoc submitByNumber(String number) {
        BizfiFiArapDoc db = mustGetByNumber(number);
        return doSubmit(db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizfiFiArapDoc audit(Long fid, String operator) {
        BizfiFiArapDoc db = mustGet(fid);
        return doAudit(db, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizfiFiArapDoc auditByNumber(String number, String operator) {
        BizfiFiArapDoc db = mustGetByNumber(number);
        return doAudit(db, operator);
    }

    @Override
    public BizfiFiArapDoc reject(Long fid, String operator) {
        BizfiFiArapDoc db = mustGet(fid);
        return doReject(db, operator);
    }

    @Override
    public BizfiFiArapDoc rejectByNumber(String number, String operator) {
        BizfiFiArapDoc db = mustGetByNumber(number);
        return doReject(db, operator);
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
    public Map<String, Object> agingSummary(String docTypeRoot, LocalDate asOfDate) {
        String root = normalizeRoot(docTypeRoot);
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        List<BizfiFiArapDoc> docs = listOpenDocs(root);

        BigDecimal b0_30 = BigDecimal.ZERO;
        BigDecimal b31_60 = BigDecimal.ZERO;
        BigDecimal b61_90 = BigDecimal.ZERO;
        BigDecimal b91Plus = BigDecimal.ZERO;

        for (BizfiFiArapDoc d : docs) {
            long days = Math.max(0, ChronoUnit.DAYS.between(d.getFdate(), date));
            BigDecimal amt = nz(d.getFamount());
            if (days <= 30) b0_30 = b0_30.add(amt);
            else if (days <= 60) b31_60 = b31_60.add(amt);
            else if (days <= 90) b61_90 = b61_90.add(amt);
            else b91Plus = b91Plus.add(amt);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("docTypeRoot", root);
        result.put("asOfDate", date);
        result.put("docCount", docs.size());
        result.put("totalAmount", b0_30.add(b31_60).add(b61_90).add(b91Plus));

        List<Map<String, Object>> buckets = new ArrayList<>();
        buckets.add(bucket("0-30", b0_30));
        buckets.add(bucket("31-60", b31_60));
        buckets.add(bucket("61-90", b61_90));
        buckets.add(bucket("91+", b91Plus));
        result.put("buckets", buckets);
        return result;
    }

    @Override
    public List<Map<String, Object>> creditWarnings(String docTypeRoot, LocalDate asOfDate) {
        String root = normalizeRoot(docTypeRoot);
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        List<BizfiFiArapDoc> docs = listOpenDocs(root);

        Map<String, List<BizfiFiArapDoc>> byCounterparty = docs.stream()
                .collect(Collectors.groupingBy(BizfiFiArapDoc::getFcounterparty));

        LambdaQueryWrapper<BizfiFiCounterpartyCredit> qw = new LambdaQueryWrapper<>();
        qw.eq(BizfiFiCounterpartyCredit::getFdocTypeRoot, root)
                .eq(BizfiFiCounterpartyCredit::getFenabled, 1);
        List<BizfiFiCounterpartyCredit> configs = creditMapper.selectList(qw);
        Map<String, BizfiFiCounterpartyCredit> configMap = configs.stream()
                .collect(Collectors.toMap(BizfiFiCounterpartyCredit::getFcounterparty, c -> c, (a, b) -> a));

        List<Map<String, Object>> warnings = new ArrayList<>();
        for (Map.Entry<String, List<BizfiFiArapDoc>> e : byCounterparty.entrySet()) {
            String counterparty = e.getKey();
            BizfiFiCounterpartyCredit conf = configMap.get(counterparty);
            if (conf == null) {
                continue;
            }
            BigDecimal totalOutstanding = e.getValue().stream()
                    .map(BizfiFiArapDoc::getFamount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long maxOverdueDays = e.getValue().stream()
                    .mapToLong(d -> Math.max(0, ChronoUnit.DAYS.between(d.getFdate(), date)))
                    .max()
                    .orElse(0);
            boolean overLimit = conf.getFcreditLimit() != null && totalOutstanding.compareTo(conf.getFcreditLimit()) > 0;
            int overdueThreshold = conf.getFoverdueDaysThreshold() == null ? 30 : conf.getFoverdueDaysThreshold();
            boolean overdue = maxOverdueDays > overdueThreshold;
            if (!overLimit && !overdue) {
                continue;
            }

            Map<String, Object> w = new LinkedHashMap<>();
            w.put("docTypeRoot", root);
            w.put("counterparty", counterparty);
            w.put("creditLimit", conf.getFcreditLimit());
            w.put("overdueDaysThreshold", overdueThreshold);
            w.put("totalOutstanding", totalOutstanding);
            w.put("maxOverdueDays", maxOverdueDays);
            w.put("overLimit", overLimit);
            w.put("overdue", overdue);
            w.put("blockOnOverLimit", conf.getFblockOnOverLimit());
            w.put("blockOnOverdue", conf.getFblockOnOverdue());
            warnings.add(w);
        }

        warnings.sort((a, b) -> ((BigDecimal) b.get("totalOutstanding")).compareTo((BigDecimal) a.get("totalOutstanding")));
        return warnings;
    }

    @Override
    public BizfiFiCounterpartyCredit saveCreditConfig(BizfiFiCounterpartyCredit config, String operator) {
        if (config == null || !StringUtils.hasText(config.getFcounterparty())) {
            throw new BizException("往来方不能为空");
        }
        String root = normalizeRoot(config.getFdocTypeRoot());
        if (config.getFcreditLimit() == null || config.getFcreditLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("信用额度必须大于0");
        }
        if (config.getFoverdueDaysThreshold() == null || config.getFoverdueDaysThreshold() < 0) {
            config.setFoverdueDaysThreshold(30);
        }
        if (config.getFenabled() == null) {
            config.setFenabled(1);
        }
        if (config.getFblockOnOverLimit() == null) {
            config.setFblockOnOverLimit(0);
        }
        if (config.getFblockOnOverdue() == null) {
            config.setFblockOnOverdue(0);
        }

        LambdaQueryWrapper<BizfiFiCounterpartyCredit> q = new LambdaQueryWrapper<>();
        q.eq(BizfiFiCounterpartyCredit::getFcounterparty, config.getFcounterparty().trim())
                .eq(BizfiFiCounterpartyCredit::getFdocTypeRoot, root)
                .last("limit 1");
        BizfiFiCounterpartyCredit db = creditMapper.selectOne(q);

        config.setFcounterparty(config.getFcounterparty().trim());
        config.setFdocTypeRoot(root);
        config.setFupdatedBy(StringUtils.hasText(operator) ? operator : "system");
        config.setFupdatedTime(LocalDateTime.now());

        if (db == null) {
            creditMapper.insert(config);
            return config;
        }

        config.setFid(db.getFid());
        creditMapper.updateById(config);
        return creditMapper.selectById(db.getFid());
    }

    @Override
    public List<BizfiFiCounterpartyCredit> listCreditConfigs(String docTypeRoot) {
        String root = normalizeRoot(docTypeRoot);
        LambdaQueryWrapper<BizfiFiCounterpartyCredit> q = new LambdaQueryWrapper<>();
        q.eq(BizfiFiCounterpartyCredit::getFdocTypeRoot, root)
                .orderByAsc(BizfiFiCounterpartyCredit::getFcounterparty);
        return creditMapper.selectList(q);
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

    private BizfiFiArapDoc mustGetByNumber(String number) {
        if (!StringUtils.hasText(number)) throw new BizException("单据号不能为空");
        LambdaQueryWrapper<BizfiFiArapDoc> q = new LambdaQueryWrapper<>();
        q.eq(BizfiFiArapDoc::getFnumber, number.trim()).last("limit 1");
        BizfiFiArapDoc db = mapper.selectOne(q);
        if (db == null) throw new BizException("单据不存在");
        return db;
    }

    private BizfiFiArapDoc doSubmit(BizfiFiArapDoc db) {
        if (!DRAFT.equals(db.getFstatus()) && !REJECTED.equals(db.getFstatus())) throw new BizException("仅草稿/驳回可提交");
        checkHardRiskBlock(db, "提交");
        db.setFstatus(SUBMITTED);
        mapper.updateById(db);
        return mapper.selectById(db.getFid());
    }

    private BizfiFiArapDoc doAudit(BizfiFiArapDoc db, String operator) {
        if (!SUBMITTED.equals(db.getFstatus())) throw new BizException("仅已提交可审核");
        checkHardRiskBlock(db, "审核");
        db.setFstatus(AUDITED);
        db.setFauditedBy(StringUtils.hasText(operator) ? operator : "system");
        db.setFauditedTime(LocalDateTime.now());
        mapper.updateById(db);
        // 审核通过后自动生成并关联凭证；若已存在关联则幂等跳过
        return generateVoucher(db.getFid(), operator);
    }

    private BizfiFiArapDoc doReject(BizfiFiArapDoc db, String operator) {
        if (!SUBMITTED.equals(db.getFstatus())) throw new BizException("仅已提交可驳回");
        db.setFstatus(REJECTED);
        db.setFauditedBy(StringUtils.hasText(operator) ? operator : "system");
        db.setFauditedTime(LocalDateTime.now());
        mapper.updateById(db);
        return mapper.selectById(db.getFid());
    }

    private void checkHardRiskBlock(BizfiFiArapDoc doc, String actionName) {
        if (doc == null || !StringUtils.hasText(doc.getFcounterparty()) || !StringUtils.hasText(doc.getFdoctype())) {
            return;
        }
        String root = normalizeRoot(doc.getFdoctype().startsWith("AP") ? "AP" : "AR");
        LambdaQueryWrapper<BizfiFiCounterpartyCredit> q = new LambdaQueryWrapper<>();
        q.eq(BizfiFiCounterpartyCredit::getFdocTypeRoot, root)
                .eq(BizfiFiCounterpartyCredit::getFcounterparty, doc.getFcounterparty())
                .eq(BizfiFiCounterpartyCredit::getFenabled, 1)
                .last("limit 1");
        BizfiFiCounterpartyCredit conf = creditMapper.selectOne(q);
        if (conf == null) {
            return;
        }

        List<Map<String, Object>> warnings = creditWarnings(root, doc.getFdate() == null ? LocalDate.now() : doc.getFdate());
        Map<String, Object> hit = warnings.stream()
                .filter(w -> doc.getFcounterparty().equals(w.get("counterparty")))
                .findFirst()
                .orElse(null);
        if (hit == null) {
            return;
        }

        boolean overLimit = Boolean.TRUE.equals(hit.get("overLimit"));
        boolean overdue = Boolean.TRUE.equals(hit.get("overdue"));
        boolean blockOverLimit = Integer.valueOf(1).equals(conf.getFblockOnOverLimit());
        boolean blockOverdue = Integer.valueOf(1).equals(conf.getFblockOnOverdue());

        if ((overLimit && blockOverLimit) || (overdue && blockOverdue)) {
            StringBuilder sb = new StringBuilder(actionName).append("已被信用规则拦截：");
            if (overLimit && blockOverLimit) {
                sb.append("超额度");
            }
            if (overdue && blockOverdue) {
                if (sb.length() > 0) sb.append(" ");
                sb.append("超逾期");
            }
            throw new BizException(sb.toString().trim());
        }
    }

    private void validate(BizfiFiArapDoc doc) {
        if (doc == null) throw new BizException("参数不能为空");
        if (!StringUtils.hasText(doc.getFdoctype())) throw new BizException("单据类型不能为空");
        if (doc.getFdate() == null) doc.setFdate(LocalDate.now());
        if (doc.getFamount() == null || doc.getFamount().compareTo(BigDecimal.ZERO) <= 0) throw new BizException("金额必须大于0");
        if (!StringUtils.hasText(doc.getFcounterparty())) throw new BizException("往来方不能为空");
    }

    private String normalizeRoot(String docTypeRoot) {
        String root = StringUtils.hasText(docTypeRoot) ? docTypeRoot.trim().toUpperCase() : "AR";
        if (!"AR".equals(root) && !"AP".equals(root)) {
            throw new BizException("docTypeRoot 仅支持 AR/AP");
        }
        return root;
    }

    private List<BizfiFiArapDoc> listOpenDocs(String docTypeRoot) {
        LambdaQueryWrapper<BizfiFiArapDoc> q = new LambdaQueryWrapper<>();
        q.likeRight(BizfiFiArapDoc::getFdoctype, docTypeRoot)
                .in(BizfiFiArapDoc::getFstatus, Arrays.asList(SUBMITTED, AUDITED))
                .orderByDesc(BizfiFiArapDoc::getFdate)
                .orderByDesc(BizfiFiArapDoc::getFid);
        return mapper.selectList(q);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Map<String, Object> bucket(String range, BigDecimal amount) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("range", range);
        b.put("amount", amount);
        return b;
    }
}
