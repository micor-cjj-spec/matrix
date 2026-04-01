package single.cjj.fi.gl.service;

import single.cjj.fi.gl.vo.LedgerQueryResultVO;

public interface BizfiFiLedgerQueryService {
    LedgerQueryResultVO subjectBalance(String startDate, String endDate, String accountCode);

    LedgerQueryResultVO generalLedger(String startDate, String endDate, String accountCode);

    LedgerQueryResultVO detailLedger(String startDate, String endDate, String accountCode);

    LedgerQueryResultVO dailyReport(String startDate, String endDate, String accountCode);

    LedgerQueryResultVO dimensionBalance(String startDate, String endDate, String accountCode, String dimensionCode);

    LedgerQueryResultVO auxDimensionBalance(String startDate, String endDate, String accountCode, String dimensionCode);

    LedgerQueryResultVO auxGeneralLedger(String startDate, String endDate, String accountCode, String dimensionCode);

    LedgerQueryResultVO auxDetailLedger(String startDate, String endDate, String accountCode, String dimensionCode);
}
