package single.cjj.fi.gl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCheckBatch;
import single.cjj.fi.gl.vo.MonthEndBatchActionRequestVO;
import single.cjj.fi.gl.vo.MonthEndBatchCreateRequestVO;

import java.util.Map;

public interface BizfiFiMonthEndCheckBatchService extends IService<BizfiFiMonthEndCheckBatch> {
    BizfiFiMonthEndCheckBatch createBatch(MonthEndBatchCreateRequestVO request);

    IPage<BizfiFiMonthEndCheckBatch> list(int page, int size, Map<String, Object> query);

    BizfiFiMonthEndCheckBatch get(Long fid);

    BizfiFiMonthEndCheckBatch submit(Long fid, MonthEndBatchActionRequestVO request);

    BizfiFiMonthEndCheckBatch approve(Long fid, MonthEndBatchActionRequestVO request);
}

