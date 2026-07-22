package single.cjj.fi.ar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ar.dto.BotpArapContracts.ArapWritebackRequest;
import single.cjj.fi.ar.dto.BotpArapContracts.PaymentApplicationCreateRequest;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.mapper.BizfiFiArapDocMapper;
import single.cjj.fi.ar.mapper.BotpArapIntegrationMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class BotpArapIntegrationService {

    private static final String AP = "AP";
    private static final String PAYMENT_APPLICATION = "AP_PAYMENT_APPLY";
    private static final String AUDITED = "AUDITED";
    private static final String DRAFT = "DRAFT";

    private final BizfiFiArapDocMapper arapDocMapper;
    private final BotpArapIntegrationMapper lockMapper;

    public BotpArapIntegrationService(
            BizfiFiArapDocMapper arapDocMapper,
            BotpArapIntegrationMapper lockMapper
    ) {
        this.arapDocMapper = arapDocMapper;
        this.lockMapper = lockMapper;
    }

    public BizfiFiArapDoc detail(Long fid) {
        BizfiFiArapDoc doc = arapDocMapper.selectById(fid);
        if (doc == null) {
            throw new BizException("单据不存在: " + fid);
        }
        return doc;
    }

    public BizfiFiArapDoc findByIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BizException("BOTP 幂等键不能为空");
        }
        return arapDocMapper.selectOne(new LambdaQueryWrapper<BizfiFiArapDoc>()
                .eq(BizfiFiArapDoc::getFbotpIdempotencyKey, idempotencyKey)
                .last("limit 1"));
    }

    @Transactional(rollbackFor = Exception.class)
    public BizfiFiArapDoc createPaymentApplication(PaymentApplicationCreateRequest request) {
        BizfiFiArapDoc existing = findByIdempotencyKey(request.idempotencyKey());
        if (existing != null) {
            return existing;
        }

        Long sourceId = parseSourceId(request.sourceDocumentId());
        BizfiFiArapDoc source = lockMapper.selectByIdForUpdate(sourceId);
        if (source == null) {
            throw new BizException("应付单不存在: " + request.sourceDocumentId());
        }
        existing = findByIdempotencyKey(request.idempotencyKey());
        if (existing != null) {
            return existing;
        }
        validateSource(source);

        BigDecimal amount = nz(source.getFamount());
        BigDecimal applied = nz(source.getFappliedAmount());
        BigDecimal reserved = nz(source.getFreservedAmount());
        BigDecimal available = amount.subtract(applied).subtract(reserved);
        if (request.amount().compareTo(available) > 0) {
            throw new BizException("下推金额超过可用金额，当前可用: " + available.stripTrailingZeros().toPlainString());
        }

        BigDecimal newReserved = reserved.add(request.amount());
        source.setFreservedAmount(newReserved);
        source.setFremainingAmount(amount.subtract(applied).subtract(newReserved));
        source.setFpushStatus(pushStatus(amount, applied.add(newReserved)));
        if (source.getFversion() == null) {
            source.setFversion(0);
        }
        arapDocMapper.updateById(source);

        BizfiFiArapDoc target = new BizfiFiArapDoc();
        target.setFdoctype(PAYMENT_APPLICATION);
        target.setFnumber(PAYMENT_APPLICATION + "-" + System.currentTimeMillis());
        target.setFdate(LocalDate.now());
        target.setFcounterparty(request.counterparty());
        target.setFamount(request.amount());
        target.setFstatus(DRAFT);
        target.setFremark("BOTP 来源: " + request.sourceBillNo());
        target.setFpayMethod(request.payMethod());
        target.setFplannedPayDate(request.plannedPayDate());
        target.setFsourceBillNo(request.sourceBillNo());
        target.setFbotpIdempotencyKey(request.idempotencyKey());
        target.setFsourceSystem(request.sourceSystem());
        target.setFsourceDocumentType(request.sourceDocumentType());
        target.setFsourceDocumentId(request.sourceDocumentId());
        target.setFsourceExecutionId(request.sourceExecutionId());
        target.setFappliedAmount(BigDecimal.ZERO);
        target.setFreservedAmount(BigDecimal.ZERO);
        target.setFremainingAmount(request.amount());
        target.setFpushStatus("NOT_PUSHED");
        target.setFversion(0);
        arapDocMapper.insert(target);
        return target;
    }

    @Transactional(rollbackFor = Exception.class)
    public BizfiFiArapDoc recomputeWriteback(Long sourceId, ArapWritebackRequest request) {
        BizfiFiArapDoc source = lockMapper.selectByIdForUpdate(sourceId);
        if (source == null) {
            throw new BizException("应付单不存在: " + sourceId);
        }
        validateSourceType(source);

        BigDecimal total = nz(source.getFamount());
        BigDecimal active = nz(request.activeAllocatedAmount());
        if (active.compareTo(total) > 0) {
            throw new BizException("有效关联金额不能超过应付金额");
        }

        BigDecimal reserved = nz(source.getFreservedAmount())
                .subtract(nz(request.releaseReservedAmount()))
                .max(BigDecimal.ZERO);
        BigDecimal remaining = total.subtract(active).subtract(reserved);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("反写后剩余金额小于0，请检查关系台账与预占金额");
        }

        source.setFappliedAmount(active);
        source.setFreservedAmount(reserved);
        source.setFremainingAmount(remaining);
        source.setFpushStatus(pushStatus(total, active.add(reserved)));
        if (source.getFversion() == null) {
            source.setFversion(0);
        }
        arapDocMapper.updateById(source);
        return arapDocMapper.selectById(sourceId);
    }

    private void validateSource(BizfiFiArapDoc source) {
        validateSourceType(source);
        if (!AUDITED.equals(source.getFstatus())) {
            throw new BizException("仅已审核应付单允许下推付款申请");
        }
        if (nz(source.getFamount()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("应付单金额必须大于0");
        }
    }

    private void validateSourceType(BizfiFiArapDoc source) {
        if (!AP.equalsIgnoreCase(source.getFdoctype())) {
            throw new BizException("源单必须是应付单 AP");
        }
    }

    private String pushStatus(BigDecimal total, BigDecimal occupied) {
        if (occupied.compareTo(BigDecimal.ZERO) <= 0) {
            return "NOT_PUSHED";
        }
        if (occupied.compareTo(total) >= 0) {
            return "COMPLETE";
        }
        return "PARTIAL";
    }

    private Long parseSourceId(String sourceDocumentId) {
        try {
            return Long.valueOf(sourceDocumentId);
        } catch (NumberFormatException exception) {
            throw new BizException("应付单ID格式错误: " + sourceDocumentId);
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
