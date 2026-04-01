package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.gl.entity.BizfiFiOrgFinanceConfig;
import single.cjj.fi.gl.mapper.BizfiFiOrgFinanceConfigMapper;
import single.cjj.fi.gl.service.BizfiFiOrgFinanceConfigService;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

/**
 * 组织财务配置服务实现
 */
@Service
public class BizfiFiOrgFinanceConfigServiceImpl
        extends ServiceImpl<BizfiFiOrgFinanceConfigMapper, BizfiFiOrgFinanceConfig>
        implements BizfiFiOrgFinanceConfigService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String PERIOD_CONTROL_STRICT = "STRICT";
    private static final String PERIOD_CONTROL_FLEXIBLE = "FLEXIBLE";

    @Autowired
    private BizfiFiOrgFinanceConfigMapper mapper;

    @Override
    public BizfiFiOrgFinanceConfig add(BizfiFiOrgFinanceConfig config) {
        normalizeAndValidate(config, null);
        ensureUnique(config.getForg(), null);
        LocalDateTime now = LocalDateTime.now();
        config.setFcreatetime(now);
        config.setFupdatetime(now);
        mapper.insert(config);
        return config;
    }

    @Override
    public BizfiFiOrgFinanceConfig update(BizfiFiOrgFinanceConfig config) {
        if (config == null || config.getFid() == null) {
            throw new BizException("组织财务配置ID不能为空");
        }
        BizfiFiOrgFinanceConfig db = get(config.getFid());
        if (db == null) {
            throw new BizException("组织财务配置不存在");
        }
        normalizeAndValidate(config, db);
        ensureUnique(config.getForg(), config.getFid());
        config.setFcreatetime(db.getFcreatetime());
        config.setFupdatetime(LocalDateTime.now());
        mapper.updateById(config);
        return mapper.selectById(config.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiOrgFinanceConfig get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public BizfiFiOrgFinanceConfig getByOrg(Long forg) {
        if (forg == null) {
            throw new BizException("组织ID不能为空");
        }
        return mapper.selectOne(new LambdaQueryWrapper<BizfiFiOrgFinanceConfig>()
                .eq(BizfiFiOrgFinanceConfig::getForg, forg));
    }

    @Override
    public IPage<BizfiFiOrgFinanceConfig> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiOrgFinanceConfig> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.get("forg") instanceof Number number) {
                wrapper.eq(BizfiFiOrgFinanceConfig::getForg, number.longValue());
            }
            if (StringUtils.hasText((String) query.get("fstatus"))) {
                wrapper.eq(BizfiFiOrgFinanceConfig::getFstatus,
                        query.get("fstatus").toString().trim().toUpperCase(Locale.ROOT));
            }
            if (StringUtils.hasText((String) query.get("fbaseCurrency"))) {
                wrapper.eq(BizfiFiOrgFinanceConfig::getFbaseCurrency,
                        query.get("fbaseCurrency").toString().trim().toUpperCase(Locale.ROOT));
            }
        }
        wrapper.orderByAsc(BizfiFiOrgFinanceConfig::getForg)
                .orderByDesc(BizfiFiOrgFinanceConfig::getFupdatetime);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    private void ensureUnique(Long forg, Long excludeId) {
        LambdaQueryWrapper<BizfiFiOrgFinanceConfig> wrapper = new LambdaQueryWrapper<BizfiFiOrgFinanceConfig>()
                .eq(BizfiFiOrgFinanceConfig::getForg, forg)
                .ne(excludeId != null, BizfiFiOrgFinanceConfig::getFid, excludeId);
        if (mapper.selectCount(wrapper) > 0) {
            throw new BizException("该组织已存在财务配置: " + forg);
        }
    }

    private void normalizeAndValidate(BizfiFiOrgFinanceConfig config, BizfiFiOrgFinanceConfig existing) {
        if (config == null) {
            throw new BizException("组织财务配置参数不能为空");
        }
        if (config.getForg() == null) {
            throw new BizException("组织ID不能为空");
        }

        String baseCurrency = StringUtils.hasText(config.getFbaseCurrency())
                ? config.getFbaseCurrency().trim().toUpperCase(Locale.ROOT) : "CNY";
        config.setFbaseCurrency(baseCurrency);

        if (StringUtils.hasText(config.getFcurrentPeriod())) {
            YearMonth yearMonth = parsePeriod(config.getFcurrentPeriod());
            if (yearMonth == null) {
                throw new BizException("当前期间格式错误，需为yyyy-MM");
            }
            config.setFcurrentPeriod(yearMonth.toString());
        } else {
            config.setFcurrentPeriod(null);
        }

        String defaultControlMode = existing != null && StringUtils.hasText(existing.getFperiodControlMode())
                ? existing.getFperiodControlMode() : PERIOD_CONTROL_STRICT;
        String controlMode = StringUtils.hasText(config.getFperiodControlMode())
                ? config.getFperiodControlMode().trim().toUpperCase(Locale.ROOT) : defaultControlMode;
        if (!PERIOD_CONTROL_STRICT.equals(controlMode) && !PERIOD_CONTROL_FLEXIBLE.equals(controlMode)) {
            throw new BizException("期间控制模式仅支持STRICT/FLEXIBLE");
        }
        config.setFperiodControlMode(controlMode);

        String defaultStatus = existing != null && StringUtils.hasText(existing.getFstatus())
                ? existing.getFstatus() : STATUS_ENABLED;
        String status = StringUtils.hasText(config.getFstatus())
                ? config.getFstatus().trim().toUpperCase(Locale.ROOT) : defaultStatus;
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BizException("组织财务配置状态仅支持ENABLED/DISABLED");
        }
        config.setFstatus(status);

        config.setFdefaultVoucherType(trimToNull(config.getFdefaultVoucherType()));
        config.setFremark(trimToNull(config.getFremark()));
    }

    private YearMonth parsePeriod(String period) {
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
