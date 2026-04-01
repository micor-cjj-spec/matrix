package single.cjj.fi.gl.service;

import single.cjj.fi.gl.vo.VoucherCarryListResultVO;
import single.cjj.fi.gl.vo.VoucherSummaryResultVO;

public interface BizfiFiVoucherAnalysisService {
    VoucherSummaryResultVO summary(String startDate, String endDate, String status, String summaryKeyword);

    VoucherCarryListResultVO carryList(Long forg, String period);
}
