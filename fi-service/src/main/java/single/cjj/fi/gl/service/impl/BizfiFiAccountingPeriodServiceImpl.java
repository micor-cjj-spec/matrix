package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.gl.entity.BizfiFiAccountingPeriod;
import single.cjj.fi.gl.mapper.BizfiFiAccountingPeriodMapper;
import single.cjj.fi.gl.service.BizfiFiAccountingPeriodService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

/**
 * 会计期间服务实现
 */
@Service
public class BizfiFiAccountingPeriodServiceImpl
        extends ServiceImpl<BizfiFiAccountingPeriodMapper, BizfiFiAccountingPeriod>
        implements BizfiFiAccountingPeriodService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_CLOSED = "CLOSED";

    @Autowired
    private BizfiFiAccountingPeriodMapper mapper;

    @Override
    public BizfiFiAccountingPeriod add(BizfiFiAccountingPeriod period) {
        normalizeAndValidate(period, null);
        ensureUnique(period.getForg(), period.getFperiod(), null);
        LocalDateTime now = LocalDateTime.now();
        period.setFcreatetime(now);
        period.setFupdatetime(now);
        mapper.insert(period);
        return period;
    }

    @Override
    public BizfiFiAccountingPeriod update(BizfiFiAccountingPeriod period) {
        if (period == null || period.getFid() == null) {
            throw new BizException("会计期间ID不能为空");
        }
        BizfiFiAccountingPeriod db = get(period.getFid());
        if (db == null) {
            throw new BizException("会计期间不存在");
        }
        normalizeAndValidate(period, db);
        ensureUnique(period.getForg(), period.getFperiod(), period.getFid());
        period.setFcreatetime(db.getFcreatetime());
        period.setFupdatetime(LocalDateTime.now());
        mapper.updateById(period);
        return mapper.selectById(period.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        BizfiFiAccountingPeriod db = get(fid);
        if (db == null) {
            return false;
        }
        if (STATUS_CLOSED.equals(db.getFstatus())) {
            throw new BizException("已关闭期间不允许删除");
        }
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiAccountingPeriod get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiAccountingPeriod> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiAccountingPeriod> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.get("forg") instanceof Number number) {
                wrapper.eq(BizfiFiAccountingPeriod::getForg, number.longValue());
            }
            if (StringUtils.hasText((String) query.get("fstatus"))) {
                wrapper.eq(BizfiFiAccountingPeriod::getFstatus,
                        query.get("fstatus").toString().trim().toUpperCase(Locale.ROOT));
            }
            if (query.get("fyear") instanceof Number number) {
                wrapper.eq(BizfiFiAccountingPeriod::getFyear, number.intValue());
            }
            if (StringUtils.hasText((String) query.get("fperiod"))) {
                wrapper.eq(BizfiFiAccountingPeriod::getFperiod, query.get("fperiod").toString().trim());
            }
        }
        wrapper.orderByDesc(BizfiFiAccountingPeriod::getFyear)
                .orderByDesc(BizfiFiAccountingPeriod::getFmonth)
                .orderByDesc(BizfiFiAccountingPeriod::getFid);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public BizfiFiAccountingPeriod close(Long fid, String operator) {
        BizfiFiAccountingPeriod db = mustGet(fid);
        if (STATUS_CLOSED.equals(db.getFstatus())) {
            return db;
        }
        db.setFstatus(STATUS_CLOSED);
        db.setFcloseBy(StringUtils.hasText(operator) ? operator.trim() : "system");
        db.setFcloseTime(LocalDateTime.now());
        db.setFupdatetime(LocalDateTime.now());
        mapper.updateById(db);
        return mapper.selectById(fid);
    }

    @Override
    public BizfiFiAccountingPeriod reopen(Long fid) {
        BizfiFiAccountingPeriod db = mustGet(fid);
        if (!STATUS_CLOSED.equals(db.getFstatus())) {
            throw new BizException("仅已关闭期间允许反开");
        }
        db.setFstatus(STATUS_OPEN);
        db.setFcloseBy(null);
        db.setFcloseTime(null);
        db.setFupdatetime(LocalDateTime.now());
        mapper.updateById(db);
        return mapper.selectById(fid);
    }

    private BizfiFiAccountingPeriod mustGet(Long fid) {
        if (fid == null) {
            throw new BizException("会计期间ID不能为空");
        }
        BizfiFiAccountingPeriod db = mapper.selectById(fid);
        if (db == null) {
            throw new BizException("会计期间不存在");
        }
        return db;
    }

    private void ensureUnique(Long forg, String period, Long excludeId) {
        LambdaQueryWrapper<BizfiFiAccountingPeriod> wrapper = new LambdaQueryWrapper<BizfiFiAccountingPeriod>()
                .eq(BizfiFiAccountingPeriod::getForg, forg)
                .eq(BizfiFiAccountingPeriod::getFperiod, period)
                .ne(excludeId != null, BizfiFiAccountingPeriod::getFid, excludeId);
        if (mapper.selectCount(wrapper) > 0) {
            throw new BizException("同组织同期间记录已存在: " + forg + "-" + period);
        }
    }

    private void normalizeAndValidate(BizfiFiAccountingPeriod period, BizfiFiAccountingPeriod existing) {
        if (period == null) {
            throw new BizException("会计期间参数不能为空");
        }
        if (period.getForg() == null) {
            throw new BizException("组织ID不能为空");
        }
        YearMonth yearMonth = parsePeriod(period.getFperiod());
        if (yearMonth == null) {
            throw new BizException("会计期间格式错误，需为yyyy-MM");
        }
        period.setFperiod(yearMonth.toString());
        period.setFyear(yearMonth.getYear());
        period.setFmonth(yearMonth.getMonthValue());

        LocalDate startDate = period.getFstartDate() != null ? period.getFstartDate() : yearMonth.atDay(1);
        LocalDate endDate = period.getFendDate() != null ? period.getFendDate() : yearMonth.atEndOfMonth();
        if (endDate.isBefore(startDate)) {
            throw new BizException("会计期间结束日期不能早于开始日期");
        }
        if (!yearMonth.equals(YearMonth.from(startDate)) || !yearMonth.equals(YearMonth.from(endDate))) {
            throw new BizException("会计期间日期范围必须落在同一月份内");
        }
        period.setFstartDate(startDate);
        period.setFendDate(endDate);

        String defaultStatus = existing != null && StringUtils.hasText(existing.getFstatus())
                ? existing.getFstatus() : STATUS_OPEN;
        String status = StringUtils.hasText(period.getFstatus())
                ? period.getFstatus().trim().toUpperCase(Locale.ROOT) : defaultStatus;
        if (!STATUS_OPEN.equals(status) && !STATUS_CLOSED.equals(status)) {
            throw new BizException("会计期间状态仅支持OPEN/CLOSED");
        }
        period.setFstatus(status);

        if (STATUS_CLOSED.equals(status)) {
            period.setFcloseBy(StringUtils.hasText(period.getFcloseBy())
                    ? period.getFcloseBy().trim()
                    : existing != null ? existing.getFcloseBy() : null);
            period.setFcloseTime(period.getFcloseTime() != null
                    ? period.getFcloseTime()
                    : existing != null && STATUS_CLOSED.equals(existing.getFstatus())
                    ? existing.getFcloseTime()
                    : LocalDateTime.now());
        } else {
            period.setFcloseBy(null);
            period.setFcloseTime(null);
        }

        period.setFremark(trimToNull(period.getFremark()));
    }

    private YearMonth parsePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            return null;
        }
        try {
            return YearMonth.parse(period.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
