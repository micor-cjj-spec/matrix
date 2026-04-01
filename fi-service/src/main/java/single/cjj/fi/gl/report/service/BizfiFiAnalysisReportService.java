package single.cjj.fi.gl.report.service;

import single.cjj.fi.gl.report.vo.EnterpriseTaxResultVO;
import single.cjj.fi.gl.report.vo.FinancialIndicatorResultVO;

public interface BizfiFiAnalysisReportService {
    FinancialIndicatorResultVO financialIndicators(Long orgId, String period, String currency, Long balanceTemplateId, Long profitTemplateId);

    EnterpriseTaxResultVO enterpriseTax(Long orgId, String period, String currency, Long profitTemplateId);
}
