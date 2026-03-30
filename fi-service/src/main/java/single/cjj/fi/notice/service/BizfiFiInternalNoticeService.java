package single.cjj.fi.notice.service;

import java.time.LocalDate;
import java.util.Map;

public interface BizfiFiInternalNoticeService {
    Map<String, Object> queryCounterpartyNotices(String docTypeRoot, String status, String severity, LocalDate asOfDate);

    Map<String, Object> generateCounterpartyNotices(String docTypeRoot, LocalDate asOfDate, String operator);

    Map<String, Object> reconcileCounterpartyNotices(String docTypeRoot, String status, LocalDate asOfDate);

    Map<String, Object> queryCashflowNotices(Long orgId, String period, String status, String severity, String sourceCode, String currency);

    Map<String, Object> generateCashflowNotices(Long orgId, String period, String currency, String operator);

    Map<String, Object> reconcileCashflowNotices(Long orgId, String period, String status, String currency);
}
