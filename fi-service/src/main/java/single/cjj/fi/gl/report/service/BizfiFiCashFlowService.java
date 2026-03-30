package single.cjj.fi.gl.report.service;

import single.cjj.fi.gl.report.vo.CashFlowSupplementResultVO;
import single.cjj.fi.gl.report.vo.CashFlowTraceResultVO;
import single.cjj.fi.gl.report.vo.ReportQueryResultVO;

public interface BizfiFiCashFlowService {
    ReportQueryResultVO query(Long orgId, String period, String currency, Long templateId, Boolean showZero);

    CashFlowTraceResultVO trace(
            Long orgId,
            String period,
            String currency,
            String cashflowItemCode,
            String categoryCode,
            String sourceType,
            String accountCode,
            String keyword
    );

    CashFlowSupplementResultVO supplement(Long orgId, String period, String currency);
}
