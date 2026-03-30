package single.cjj.fi.gl.service;

import java.util.Map;

public interface BizfiFiLedgerCollaborationService {
    Map<String, Object> voucherRules(String docTypeRoot, String keyword);

    Map<String, Object> offsetVouchers(String startDate, String endDate, String matchStatus, String keyword);

    Map<String, Object> voucherChecks(String startDate, String endDate, String issueCode, String severity, String status, Boolean onlyIssue);

    Map<String, Object> subjectBalanceCompare(String startDate, String endDate, String accountCode, Boolean diffOnly);
}
