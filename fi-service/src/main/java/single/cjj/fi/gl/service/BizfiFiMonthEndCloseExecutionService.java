package single.cjj.fi.gl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCloseExecution;

import java.util.Map;

public interface BizfiFiMonthEndCloseExecutionService extends IService<BizfiFiMonthEndCloseExecution> {
    IPage<BizfiFiMonthEndCloseExecution> list(int page, int size, Map<String, Object> query);
}
