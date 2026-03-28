package single.cjj.fi.gl.report.service;

import single.cjj.fi.gl.report.vo.ReportQueryResultVO;

public interface BizfiFiProfitStatementService {
    ReportQueryResultVO query(Long orgId, String startPeriod, String endPeriod, String currency, Long templateId, Boolean showZero);
}
