package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCloseExecution;
import single.cjj.fi.gl.mapper.BizfiFiMonthEndCloseExecutionMapper;
import single.cjj.fi.gl.service.BizfiFiMonthEndCloseExecutionService;

import java.util.Locale;
import java.util.Map;

@Service
public class BizfiFiMonthEndCloseExecutionServiceImpl
        extends ServiceImpl<BizfiFiMonthEndCloseExecutionMapper, BizfiFiMonthEndCloseExecution>
        implements BizfiFiMonthEndCloseExecutionService {

    @Override
    public IPage<BizfiFiMonthEndCloseExecution> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiMonthEndCloseExecution> wrapper = new LambdaQueryWrapper<>();
        if (query != null && query.get("forg") instanceof Number number) {
            wrapper.eq(BizfiFiMonthEndCloseExecution::getForg, number.longValue());
        }
        if (query != null && query.get("batchId") instanceof Number number) {
            wrapper.eq(BizfiFiMonthEndCloseExecution::getFbatchId, number.longValue());
        }
        if (query != null && StringUtils.hasText((String) query.get("period"))) {
            wrapper.eq(BizfiFiMonthEndCloseExecution::getFperiod, query.get("period").toString().trim());
        }
        if (query != null && StringUtils.hasText((String) query.get("executionStatus"))) {
            wrapper.eq(BizfiFiMonthEndCloseExecution::getFexecutionStatus,
                    query.get("executionStatus").toString().trim().toUpperCase(Locale.ROOT));
        }
        wrapper.orderByDesc(BizfiFiMonthEndCloseExecution::getFexecutedTime)
                .orderByDesc(BizfiFiMonthEndCloseExecution::getFid);
        return baseMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
    }
}
