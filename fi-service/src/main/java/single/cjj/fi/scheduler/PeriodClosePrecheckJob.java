package single.cjj.fi.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.service.BizfiFiLedgerCollaborationService;
import single.cjj.scheduler.client.annotation.MatrixJobHandler;
import single.cjj.scheduler.client.core.JobContext;
import single.cjj.scheduler.client.core.JobResult;
import single.cjj.scheduler.client.core.MatrixJob;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@MatrixJobHandler(value = "period-close-precheck", name = "期末结账预检查")
public class PeriodClosePrecheckJob implements MatrixJob {

    private final BizfiFiVoucherMapper voucherMapper;
    private final BizfiFiLedgerCollaborationService collaborationService;

    public PeriodClosePrecheckJob(BizfiFiVoucherMapper voucherMapper,
                                  BizfiFiLedgerCollaborationService collaborationService) {
        this.voucherMapper = voucherMapper;
        this.collaborationService = collaborationService;
    }

    @Override
    public JobResult execute(JobContext context) {
        YearMonth period = YearMonth.parse(context.getRequiredString("period"));
        String bookId = context.getString("bookId");
        String startDate = period.atDay(1).toString();
        String endDate = period.atEndOfMonth().toString();

        context.reportProgress(10, "VALIDATING", "正在校验结账期间");
        long unpostedCount = voucherMapper.selectCount(
                new LambdaQueryWrapper<BizfiFiVoucher>()
                        .ge(BizfiFiVoucher::getFdate, period.atDay(1))
                        .le(BizfiFiVoucher::getFdate, period.atEndOfMonth())
                        .eq(StringUtils.hasText(bookId), BizfiFiVoucher::getBookId, bookId)
                        .in(BizfiFiVoucher::getFstatus, "DRAFT", "SUBMITTED", "AUDITED", "REJECTED"));

        context.reportProgress(35, "CHECKING_VOUCHERS", "正在检查凭证完整性与借贷平衡");
        Map<String, Object> voucherCheck = collaborationService.voucherChecks(
                startDate, endDate, null, null, null, true);
        long issueCount = number(voucherCheck.get("issueCount"));
        long highCount = number(voucherCheck.get("highCount"));

        context.reportProgress(70, "RECONCILING_GL", "正在对照凭证分录与总账分录");
        Map<String, Object> balanceCompare = collaborationService.subjectBalanceCompare(
                startDate, endDate, null, true);
        long diffAccountCount = number(balanceCompare.get("diffAccountCount"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period.toString());
        result.put("bookId", bookId);
        result.put("unpostedVoucherCount", unpostedCount);
        result.put("voucherIssueCount", issueCount);
        result.put("highRiskIssueCount", highCount);
        result.put("diffAccountCount", diffAccountCount);
        result.put("voucherCheck", voucherCheck);
        result.put("subjectBalanceCompare", balanceCompare);
        boolean passed = unpostedCount == 0 && issueCount == 0 && diffAccountCount == 0;
        result.put("passed", passed);

        context.reportProgress(95, "SUMMARIZING", "正在汇总结账预检查结果");
        if (!passed) {
            return new JobResult(false,
                    "PERIOD_CLOSE_BLOCKED",
                    "期末结账预检查未通过：未过账凭证" + unpostedCount
                            + "张，凭证问题" + issueCount
                            + "项，科目差异" + diffAccountCount + "项",
                    result);
        }
        return JobResult.success(result);
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(String.valueOf(value));
    }
}
