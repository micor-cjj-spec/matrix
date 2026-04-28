package single.cjj.fi.gl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.entity.BizfiFiPeriodRollover;
import single.cjj.fi.gl.vo.PeriodRolloverRequestVO;
import single.cjj.fi.gl.vo.PeriodRolloverResultVO;

import java.util.Map;

public interface BizfiFiPeriodRolloverService extends IService<BizfiFiPeriodRollover> {
    PeriodRolloverResultVO rolloverFromCloseExecution(Long executionId, PeriodRolloverRequestVO request);

    IPage<BizfiFiPeriodRollover> list(int page, int size, Map<String, Object> query);
}
