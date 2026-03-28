package single.cjj.fi.gl.report.service;

import single.cjj.fi.gl.report.vo.ReportQueryResultVO;

public interface BizfiFiCashFlowService {
    ReportQueryResultVO query(Long orgId, String period, String currency, Long templateId, Boolean showZero);
}
