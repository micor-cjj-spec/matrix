package single.cjj.fi.scheduler;

import org.springframework.stereotype.Component;
import single.cjj.fi.gl.service.BizfiFiLedgerCollaborationService;
import single.cjj.scheduler.client.annotation.MatrixJobHandler;
import single.cjj.scheduler.client.core.JobContext;
import single.cjj.scheduler.client.core.JobResult;
import single.cjj.scheduler.client.core.MatrixJob;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@MatrixJobHandler(value = "voucher-period-check", name = "凭证期间检查")
public class VoucherPeriodCheckJob implements MatrixJob {

    private final BizfiFiLedgerCollaborationService collaborationService;

    public VoucherPeriodCheckJob(BizfiFiLedgerCollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @Override
    public JobResult execute(JobContext context) {
        YearMonth period = YearMonth.parse(context.getRequiredString("period"));
        context.reportProgress(10, "VALIDATING", "正在校验任务参数");
        String startDate = period.atDay(1).toString();
        String endDate = period.atEndOfMonth().toString();

        context.reportProgress(35, "LOADING_VOUCHERS", "正在加载期间凭证与分录");
        Map<String, Object> checkResult = collaborationService.voucherChecks(
                startDate, endDate, null, null, null, true);

        context.reportProgress(85, "ANALYSING", "正在汇总凭证检查结果");
        long issueCount = number(checkResult.get("issueCount"));
        long highCount = number(checkResult.get("highCount"));
        Map<String, Object> data = new LinkedHashMap<>(checkResult);
        data.put("period", period.toString());
        data.put("bookId", context.getString("bookId"));
        data.put("passed", issueCount == 0);

        if (issueCount > 0) {
            return new JobResult(false,
                    "VOUCHER_PERIOD_CHECK_FAILED",
                    "期间凭证检查发现" + issueCount + "项问题，其中高风险" + highCount + "项",
                    data);
        }
        return JobResult.success(data);
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(String.valueOf(value));
    }
}
