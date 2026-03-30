package single.cjj.fi.ar.service;

import java.time.LocalDate;
import java.util.Map;

public interface BizfiFiArapManageService {
    Map<String, Object> plan(String docTypeRoot, String counterparty, LocalDate asOfDate, Boolean auditedOnly);

    Map<String, Object> autoWriteoff(String docTypeRoot, String counterparty, LocalDate asOfDate, String operator);

    Map<String, Object> statement(String docTypeRoot, String counterparty, LocalDate asOfDate, Boolean openOnly);

    Map<String, Object> accountQuery(
            String docTypeRoot,
            String counterparty,
            String docType,
            String status,
            Boolean openOnly,
            String keyword,
            LocalDate asOfDate
    );

    Map<String, Object> writeoffLog(
            String docTypeRoot,
            String counterparty,
            String planCode,
            LocalDate startDate,
            LocalDate endDate
    );

    Map<String, Object> agingAnalysis(String docTypeRoot, LocalDate asOfDate);

    Map<String, Object> multiAnalysis(String docTypeRoot, String groupDimension, LocalDate asOfDate);
}
