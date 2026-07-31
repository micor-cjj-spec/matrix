package single.cjj.fi.ai.tool;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.fi.ai.tool.audit.FinanceAiToolAuditService;
import single.cjj.fi.gl.service.BizfiFiPeriodProcessService;
import single.cjj.fi.gl.vo.MonthEndWorkbenchResultVO;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/internal/ai/tools")
public class FinanceAiToolController {

    private static final String MONTH_END_CLOSE_CHECK = "month-end-close-check";

    private final FinanceAiToolTokenGuard tokenGuard;
    private final BizfiFiPeriodProcessService periodProcessService;
    private final FinanceMonthEndCloseToolMapper mapper;
    private final FinanceAiToolAuditService auditService;

    public FinanceAiToolController(
            FinanceAiToolTokenGuard tokenGuard,
            BizfiFiPeriodProcessService periodProcessService,
            FinanceMonthEndCloseToolMapper mapper,
            FinanceAiToolAuditService auditService
    ) {
        this.tokenGuard = tokenGuard;
        this.periodProcessService = periodProcessService;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @PostMapping("/month-end-close-check")
    public FinanceMonthEndCloseToolResponse monthEndCloseCheck(
            @RequestHeader(value = FinanceAiToolTokenGuard.HEADER_NAME, required = false) String internalToken,
            @Valid @RequestBody FinanceMonthEndCloseToolRequest request
    ) {
        tokenGuard.verify(internalToken);
        long startedAt = System.nanoTime();
        auditService.recordStarted(MONTH_END_CLOSE_CHECK, request);
        try {
            MonthEndWorkbenchResultVO result = periodProcessService.monthEndWorkbench(
                    request.organizationId(),
                    request.period()
            );
            FinanceMonthEndCloseToolResponse response = mapper.map(result);
            auditService.recordSucceeded(
                    MONTH_END_CLOSE_CHECK,
                    request,
                    response,
                    elapsedMillis(startedAt)
            );
            return response;
        } catch (RuntimeException failure) {
            auditService.recordFailed(
                    MONTH_END_CLOSE_CHECK,
                    request,
                    failure,
                    elapsedMillis(startedAt)
            );
            throw failure;
        }
    }

    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - startedAtNanos));
    }
}
