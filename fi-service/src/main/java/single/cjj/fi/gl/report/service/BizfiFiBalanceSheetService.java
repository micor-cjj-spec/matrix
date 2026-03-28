package single.cjj.fi.gl.report.service;

import single.cjj.fi.gl.report.vo.ReportQueryResultVO;
import single.cjj.fi.gl.report.vo.BalanceSheetDrillResultVO;

public interface BizfiFiBalanceSheetService {
    ReportQueryResultVO query(Long orgId, String period, String currency, Long templateId, Boolean showZero);

    BalanceSheetDrillResultVO drill(Long orgId, String period, String currency, Long templateId, Long itemId, String itemCode);
}
