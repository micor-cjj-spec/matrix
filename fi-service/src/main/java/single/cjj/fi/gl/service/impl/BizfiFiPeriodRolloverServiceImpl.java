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
import single.cjj.fi.gl.entity.BizfiFiAccountingPeriod;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCloseExecution;
import single.cjj.fi.gl.entity.BizfiFiOrgFinanceConfig;
import single.cjj.fi.gl.entity.BizfiFiPeriodRollover;
import single.cjj.fi.gl.mapper.BizfiFiAccountingPeriodMapper;
import single.cjj.fi.gl.mapper.BizfiFiMonthEndCloseExecutionMapper;
import single.cjj.fi.gl.mapper.BizfiFiOrgFinanceConfigMapper;
import single.cjj.fi.gl.mapper.BizfiFiPeriodRolloverMapper;
import single.cjj.fi.gl.service.BizfiFiAccountingPeriodService;
import single.cjj.fi.gl.service.BizfiFiPeriodRolloverService;
import single.cjj.fi.gl.vo.PeriodRolloverRequestVO;
import single.cjj.fi.gl.vo.PeriodRolloverResultVO;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BizfiFiPeriodRolloverServiceImpl
        extends ServiceImpl<BizfiFiPeriodRolloverMapper, BizfiFiPeriodRollover>
        implements BizfiFiPeriodRolloverService {

    private static final String SUCCESS = "SUCCESS";
    private static final String CLOSED = "CLOSED";
    private static final String OPEN = "OPEN";
    private static final String ENABLED = "ENABLED";
    private static final DateTimeFormatter ROLLOVER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private BizfiFiMonthEndCloseExecutionMapper closeExecutionMapper;

    @Autowired
    private BizfiFiAccountingPeriodMapper accountingPeriodMapper;

    @Autowired
    private BizfiFiAccountingPeriodService accountingPeriodService;

    @Autowired
    private BizfiFiOrgFinanceConfigMapper orgFinanceConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PeriodRolloverResultVO rolloverFromCloseExecution(Long executionId, PeriodRolloverRequestVO request) {
        if (executionId == null) {
            throw new BizException("关账执行记录ID不能为空");
        }
        BizfiFiMonthEndCloseExecution execution = closeExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BizException("关账执行记录不存在");
        }
        if (!SUCCESS.equals(normalize(execution.getFexecutionStatus()))) {
            throw new BizException("只有成功的关账执行记录可以启用下一期间");
        }
        if (!CLOSED.equals(normalize(execution.getFafterStatus()))) {
            throw new BizException("关账执行记录不是已关闭结果，不能启用下一期间");
        }
        if (baseMapper.selectCount(new LambdaQueryWrapper<BizfiFiPeriodRollover>()
                .eq(BizfiFiPeriodRollover::getFcloseExecutionId, executionId)) > 0) {
            throw new BizException("该关账执行记录已经启用过下一期间");
        }
        if (execution.getForg() == null) {
            throw new BizException("关账执行记录缺少业务单元");
        }

        YearMonth fromYearMonth = parsePeriod(execution.getFperiod());
        if (fromYearMonth == null) {
            throw new BizException("关账执行记录期间格式错误");
        }
        String fromPeriod = fromYearMonth.toString();
        String toPeriod = fromYearMonth.plusMonths(1).toString();

        BizfiFiAccountingPeriod closedPeriod = loadAccountingPeriod(execution.getForg(), fromPeriod);
        if (closedPeriod == null || !CLOSED.equals(normalize(closedPeriod.getFstatus()))) {
            throw new BizException("被关账期间当前不是CLOSED状态，不能启用下一期间");
        }

        BizfiFiOrgFinanceConfig config = loadOrgFinanceConfig(execution.getForg());
        if (config == null) {
            throw new BizException("组织财务参数不存在，不能启用下一期间");
        }
        if (!ENABLED.equals(normalize(config.getFstatus()))) {
            throw new BizException("组织财务参数未启用，不能启用下一期间");
        }
        String beforeCurrentPeriod = normalizePeriod(config.getFcurrentPeriod());
        if (!fromPeriod.equals(beforeCurrentPeriod)) {
            throw new BizException("组织财务参数当前期间已不等于被关账期间，不能继续滚动");
        }

        boolean createdNextPeriod = false;
        BizfiFiAccountingPeriod nextPeriod = loadAccountingPeriod(execution.getForg(), toPeriod);
        if (nextPeriod == null) {
            BizfiFiAccountingPeriod period = new BizfiFiAccountingPeriod();
            period.setForg(execution.getForg());
            period.setFperiod(toPeriod);
            period.setFstatus(OPEN);
            period.setFremark("期间滚动自动创建");
            nextPeriod = accountingPeriodService.add(period);
            createdNextPeriod = true;
        } else if (!OPEN.equals(normalize(nextPeriod.getFstatus()))) {
            throw new BizException("下一会计期间不是OPEN状态，不能启用");
        }

        LocalDateTime now = LocalDateTime.now();
        config.setFcurrentPeriod(toPeriod);
        config.setFupdatetime(now);
        orgFinanceConfigMapper.updateById(config);
        BizfiFiOrgFinanceConfig updatedConfig = orgFinanceConfigMapper.selectById(config.getFid());

        BizfiFiPeriodRollover rollover = new BizfiFiPeriodRollover();
        rollover.setFrolloverNo(buildRolloverNo(now));
        rollover.setFcloseExecutionId(execution.getFid());
        rollover.setFcloseExecutionNo(execution.getFexecutionNo());
        rollover.setForg(execution.getForg());
        rollover.setFfromPeriod(fromPeriod);
        rollover.setFtoPeriod(toPeriod);
        rollover.setFnextPeriodId(nextPeriod.getFid());
        rollover.setFconfigId(config.getFid());
        rollover.setFbeforeCurrentPeriod(beforeCurrentPeriod);
        rollover.setFafterCurrentPeriod(toPeriod);
        rollover.setFcreatedNextPeriod(createdNextPeriod);
        rollover.setFrolloverStatus(SUCCESS);
        rollover.setFoperator(operatorOrDefault(request == null ? null : request.getOperator()));
        rollover.setFremark(trimToNull(request == null ? null : request.getRemark()));
        rollover.setFrolledTime(now);
        rollover.setFcreatedTime(now);
        baseMapper.insert(rollover);

        return new PeriodRolloverResultVO(rollover, execution, nextPeriod, updatedConfig);
    }

    @Override
    public IPage<BizfiFiPeriodRollover> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiPeriodRollover> wrapper = new LambdaQueryWrapper<>();
        if (query != null && query.get("forg") instanceof Number number) {
            wrapper.eq(BizfiFiPeriodRollover::getForg, number.longValue());
        }
        if (query != null && query.get("closeExecutionId") instanceof Number number) {
            wrapper.eq(BizfiFiPeriodRollover::getFcloseExecutionId, number.longValue());
        }
        if (query != null && StringUtils.hasText(stringValue(query.get("fromPeriod")))) {
            wrapper.eq(BizfiFiPeriodRollover::getFfromPeriod, stringValue(query.get("fromPeriod")).trim());
        }
        if (query != null && StringUtils.hasText(stringValue(query.get("toPeriod")))) {
            wrapper.eq(BizfiFiPeriodRollover::getFtoPeriod, stringValue(query.get("toPeriod")).trim());
        }
        if (query != null && StringUtils.hasText(stringValue(query.get("rolloverStatus")))) {
            wrapper.eq(BizfiFiPeriodRollover::getFrolloverStatus,
                    stringValue(query.get("rolloverStatus")).trim().toUpperCase(Locale.ROOT));
        }
        wrapper.orderByDesc(BizfiFiPeriodRollover::getFrolledTime)
                .orderByDesc(BizfiFiPeriodRollover::getFid);
        return baseMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
    }

    private BizfiFiAccountingPeriod loadAccountingPeriod(Long forg, String period) {
        if (forg == null || !StringUtils.hasText(period)) {
            return null;
        }
        return accountingPeriodMapper.selectOne(new LambdaQueryWrapper<BizfiFiAccountingPeriod>()
                .eq(BizfiFiAccountingPeriod::getForg, forg)
                .eq(BizfiFiAccountingPeriod::getFperiod, period)
                .last("limit 1"));
    }

    private BizfiFiOrgFinanceConfig loadOrgFinanceConfig(Long forg) {
        if (forg == null) {
            return null;
        }
        return orgFinanceConfigMapper.selectOne(new LambdaQueryWrapper<BizfiFiOrgFinanceConfig>()
                .eq(BizfiFiOrgFinanceConfig::getForg, forg)
                .last("limit 1"));
    }

    private String buildRolloverNo(LocalDateTime time) {
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "PEROLL-" + ROLLOVER_TIME_FORMATTER.format(time) + "-" + randomPart;
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

    private String normalizePeriod(String period) {
        YearMonth yearMonth = parsePeriod(period);
        return yearMonth == null ? "" : yearMonth.toString();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String operatorOrDefault(String operator) {
        return StringUtils.hasText(operator) ? operator.trim() : "SYSTEM";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
